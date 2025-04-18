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

  data class EventHandlerDefinition(
    val event: KClass<out IEvent>,
    val system: KClass<out AbstractSystem>,
    val function: KFunction<*>,
    val nodes: Map<KParameter, NodeDefinition>
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
          val nodes = nodeParameters.associateWith { parameter ->
            val type = parameter.type
            if(!type.kotlinClass.isSubclassOf(Node::class)) {
              throw IllegalArgumentException("${system.qualifiedName}::${function.name} parameter ${parameter.name} illegal type $type")
            }
            nodeBuilder.getNodeDefinition(type)
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
      logger.info { "Discovered event handler: ${handler.system.qualifiedName}::${handler.function.name} for ${handler.event.qualifiedName} with ${handler.nodes.values}" }
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
    val node = nodeBuilder.tryBuild(nodeDefinition, gameObject.models.values.map { it.provide(gameObject, sender) }.toSet())
    if(node == null) return null

    node.init(sender, gameObject)
    return node
  }

  /**
   * @return `true` if all nodes were built successfully
   */
  private fun buildNodes(
    sender: SpaceChannel,
    gameObject: IGameObject,
    handler: EventHandlerDefinition,
    args: MutableMap<KParameter, Any?>
  ): Boolean {
    for((parameter, nodeDefinition) in handler.nodes) {
      val joinAll = parameter.hasAnnotation<JoinAll>()
      var node: Node? = null
      if(!joinAll) {
        node = tryProvideNode(sender, nodeDefinition, gameObject)
      } else {
        val objects = sender.space.objects.all
        for(nodeGameObject in objects) {
          node = tryProvideNode(sender, nodeDefinition, nodeGameObject)
          if(node != null) break
        }
      }

      if(node == null) {
        if(handler.mandatory) throw IllegalArgumentException("Failed to build node $nodeDefinition")

        logger.trace { "Failed to build node $nodeDefinition" }
        return false
      }

      args[parameter] = node
      logger.trace { "Built node $node for ${parameter.name}" }
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
