/*
 * Narukami TO - a server software reimplementation for a certain browser tank game.
 * Copyright (c) 2025  Daniil Pryima
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jp.assasans.narukami.server.core.impl

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.time.Duration.Companion.seconds
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import jp.assasans.narukami.server.battlefield.BattlefieldSystem
import jp.assasans.narukami.server.battleselect.BattleCreateSystem
import jp.assasans.narukami.server.battleselect.BattleSelectSystem
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherSystem
import jp.assasans.narukami.server.entrance.LoginSystem
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.lobby.LobbySystem
import jp.assasans.narukami.server.lobby.communication.ChatSystem
import jp.assasans.narukami.server.net.SpaceChannel

data class ScheduledEvent(
  val event: IEvent,
  val sender: SpaceChannel,
  val gameObject: IGameObject
)

class EventScheduler(private val scope: CoroutineScope) : IEventScheduler {
  private val logger = KotlinLogging.logger { }

  private val eventQueue = Channel<ScheduledEvent>(Channel.UNLIMITED)
  private val eventJob = scope.launch {
    for(scheduledEvent in eventQueue) {
      try {
        process(scheduledEvent.event, scheduledEvent.sender, scheduledEvent.gameObject)
      } catch(exception: Exception) {
        logger.error(exception) { "Error processing event $scheduledEvent" }
      }
    }
  }

  override fun schedule(event: IEvent, sender: SpaceChannel, gameObject: IGameObject) {
    val scheduledEvent = ScheduledEvent(event, sender, gameObject)

    logger.debug { "Scheduling event $scheduledEvent" }
    eventQueue.trySend(scheduledEvent).onFailure {
      logger.error(it) { "Failed to schedule event $scheduledEvent" }
    }
  }

  override suspend fun process(event: IEvent, sender: SpaceChannel, gameObject: IGameObject) {
    if(event is IClientEvent) {
      sender.sendBatched {
        event.attach(gameObject).enqueue()
      }
    } else {
      processServerEvent(event, sender, gameObject)
    }
  }

  data class NodeParameterDefinition(
    val parameter: KParameter,
    val nodeDefinition: NodeDefinition,
    val isList: Boolean,
  )

  data class EventHandlerDefinition(
    val event: KClass<out IEvent>,
    val system: KClass<out AbstractSystem>,
    val function: KFunction<*>,
    val nodes: List<NodeParameterDefinition>,
  ) {
    val mandatory = function.hasAnnotation<Mandatory>()
    val outOfOrder = function.hasAnnotation<OutOfOrderExecution>()
  }

  private val nodeBuilder = NodeBuilder()
  private val systems: List<KClass<out AbstractSystem>>
  private val handlers: List<EventHandlerDefinition>

  init {
    systems = listOf(
      DispatcherSystem::class,
      LoginSystem::class,
      LobbySystem::class,
      ChatSystem::class,
      BattleSelectSystem::class,
      BattleCreateSystem::class,
      BattlefieldSystem::class,
    )

    handlers = systems.flatMap { system ->
      system.declaredFunctions
        .filter { function -> function.hasAnnotation<OnEventFire>() }
        .map { function ->
          val parameters = function.valueParameters
          val eventClass = parameters[0].type.kotlinClass
          if(!eventClass.isSubclassOf(IEvent::class)) {
            throw IllegalArgumentException("${system.qualifiedName}::${function.name} first parameter is not an event type, got $eventClass")
          }

          val nodeParameters = parameters.drop(1)
          val nodes = nodeParameters.map { parameter ->
            var type = parameter.type
            var isList = false
            if(type.kotlinClass.isSubclassOf(List::class)) {
              type = requireNotNull(type.arguments[0].type) {
                "${system.qualifiedName}::${function.name} parameter ${parameter.name} has an illegal List<T> type argument"
              }
              isList = true
            }
            if(!type.kotlinClass.isSubclassOf(Node::class)) {
              throw IllegalArgumentException("${system.qualifiedName}::${function.name} parameter ${parameter.name} illegal type $type")
            }

            val nodeDefinition = nodeBuilder.getNodeDefinition(type)
            NodeParameterDefinition(parameter, nodeDefinition, isList)
          }

          @Suppress("UNCHECKED_CAST")
          EventHandlerDefinition(
            event = eventClass as KClass<out IEvent>,
            system,
            function,
            nodes,
          )
        }
    }

    for(handler in handlers) {
      logger.info { "Discovered event handler: ${handler.system.qualifiedName}::${handler.function.name} for ${handler.event.qualifiedName} with ${handler.nodes.map { it.nodeDefinition }}" }
    }
  }

  private suspend fun processServerEvent(event: IEvent, sender: SpaceChannel, gameObject: IGameObject) {
    logger.info { "Processing server event: $event" }

    for(handler in handlers) {
      if(!event::class.isSubclassOf(handler.event)) continue
      logger.info { "Trying handler ${handler.system.qualifiedName}::${handler.function.name} for $event" }

      val args = mutableMapOf<KParameter, Any?>()
      args[handler.function.valueParameters[0]] = event

      if(!buildNodes(sender, gameObject, handler, args)) {
        continue
      }

      val instanceParameter = requireNotNull(handler.function.instanceParameter) {
        "${handler.system.qualifiedName}::${handler.function.name} is not an instance method"
      }
      val instance = handler.system.createInstance()
      args[instanceParameter] = instance

      invokeHandler(event, sender, handler, args)
    }
  }

  /**
   * @return `null` if node was not built
   */
  private fun tryProvideNode(
    sender: SpaceChannel,
    nodeDefinition: NodeDefinition,
    gameObject: IGameObject
  ): Node? {
    logger.trace { "Trying to build node $nodeDefinition" }
    val node = nodeBuilder.tryBuildLazy(
      nodeDefinition,
      gameObject.models.mapValues { (_, model) ->
        { model.provide(gameObject, sender) }
      },
      gameObject.components
    )
    if(node == null) return null

    node.init(sender, gameObject)
    return node
  }

  /**
   * @return `true` if all nodes were built successfully
   */
  private fun buildNodes(
    sender: SpaceChannel,
    contextGameObject: IGameObject,
    handler: EventHandlerDefinition,
    args: MutableMap<KParameter, Any?>
  ): Boolean {
    for(nodeParameter in handler.nodes) {
      val (parameter, nodeDefinition) = nodeParameter

      val joinAll = parameter.hasAnnotation<JoinAll>()
      if(joinAll) {
        val objects = sender.space.objects.all
        val nodes = mutableListOf<Node>()
        for(nodeGameObject in objects) {
          val node = tryProvideNode(sender, nodeDefinition, nodeGameObject)
          if(node != null) {
            nodes.add(node)
          }
        }

        if(nodeParameter.isList) {
          args[parameter] = nodes
          logger.trace { "Built @JoinAll nodes $nodes for ${parameter.name}" }
        } else {
          val node = when(nodes.size) {
            0    -> {
              if(handler.mandatory) throw IllegalArgumentException("Failed to build node $nodeDefinition for ${handler.system.qualifiedName}::${handler.function.name}")

              logger.trace { "Failed to build node $nodeDefinition" }
              return false
            }

            1    -> nodes[0]
            else -> throw IllegalArgumentException("Expected one game object for ${parameter.name} of ${handler.system.qualifiedName}::${handler.function.name}, got ${nodes.size}")
          }

          args[parameter] = node
          logger.trace { "Built @JoinAll node $node for ${parameter.name}" }
        }
      } else {
        val node = tryProvideNode(sender, nodeDefinition, contextGameObject)
        if(node == null) {
          if(handler.mandatory) throw IllegalArgumentException("Failed to build context node $nodeDefinition for ${handler.system.qualifiedName}::${handler.function.name}, got $contextGameObject")

          logger.trace { "Failed to build context node $nodeDefinition" }
          return false
        }

        args[parameter] = node
        logger.trace { "Built context node $node for ${parameter.name}" }
      }
    }

    return true
  }

  private suspend fun invokeHandler(
    event: IEvent,
    sender: SpaceChannel,
    handler: EventHandlerDefinition,
    args: Map<KParameter, Any?>,
  ) {
    logger.trace { "Invoking ${handler.system.qualifiedName}::${handler.function.name} with ${args.mapKeys { (parameter, _) -> parameter.name }}" }
    if(handler.function.isSuspend) {
      if(handler.outOfOrder) {
        sender.socket.launch {
          handler.function.callSuspendBy(args)
        }
      } else {
        val timeout = 5.seconds
        val job = scope.launch {
          delay(timeout)
          logger.error { "${handler.system.qualifiedName}::${handler.function.name} timed out ($timeout) while processing $event" }
          logger.error { "Possible deadlock detected, try marking event handler with @${OutOfOrderExecution::class.simpleName} to not suspend incoming command queue" }
        }

        try {
          handler.function.callSuspendBy(args)
          logger.trace { "${handler.system.qualifiedName}::${handler.function.name} processed $event" }
        } finally {
          job.cancelAndJoin()
        }
      }
    } else {
      handler.function.callBy(args)
    }
  }
}
