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

  // Because it looks awful
  @Suppress("FoldInitializerAndIfToElvis")
  private suspend fun processServerEvent(event: IEvent, sender: SpaceChannel, gameObject: IGameObject) {
    logger.info { "Processing server event: $event" }

    val systems = listOf(
      DispatcherSystem::class,
      LoginSystem::class,
      LobbySystem::class,
      ChatSystem::class,
      BattleSelectSystem::class,
    )

    for(system in systems) {
      val methods = system.declaredFunctions
      method@ for(method in methods) {
        if(!method.hasAnnotation<OnEventFire>()) {
          logger.trace { "Method $method is not annotated with @OnEventFire" }
          continue
        }

        val instanceParameter = method.instanceParameter
        if(instanceParameter == null) {
          throw IllegalArgumentException("Method $method is not an instance method")
        }

        val outOfOrder = method.hasAnnotation<OutOfOrderExecution>()
        val mandatory = method.hasAnnotation<Mandatory>()
        val parameters = method.valueParameters

        val eventClass = parameters[0].type.kotlinClass
        if(eventClass != event::class) {
          logger.trace { "Method $method expects event of type ${eventClass.qualifiedName}, but received event is of type ${event::class.qualifiedName}" }
          continue
        }

        logger.info { "parameters[1]: ${parameters[1].type}" }

        val nodeType = parameters[1].type
        if(!nodeType.kotlinClass.isSubclassOf(Node::class)) {
          throw IllegalArgumentException("Method $method expects second parameter to be ${Node::class.qualifiedName}, but declared type is $nodeType")
        }

        val nodeBuilder = NodeBuilder()
        val nodeDefinition = nodeBuilder.getNodeDefinition(nodeType)

        logger.trace { "Trying to build node $nodeDefinition" }
        val node = nodeBuilder.tryBuild(nodeDefinition, gameObject.models.values.map { it.provide(gameObject, sender) }.toSet())
        if(node == null) {
          if(mandatory) {
            throw IllegalArgumentException("Failed to build node $nodeDefinition")
          } else {
            logger.trace { "Failed to build node $nodeDefinition" }
            continue
          }
        }

        node.init(sender, gameObject)
        logger.trace { "Built node $node" }

        val args = mutableMapOf<KParameter, Any?>()
        args[parameters[0]] = event
        args[parameters[1]] = node

        for(parameter in parameters.filter { it.hasAnnotation<JoinAll>() }) {
          val nodeType = parameter.type
          if(!nodeType.kotlinClass.isSubclassOf(Node::class)) {
            throw IllegalArgumentException("Method $method expects $parameter to be ${Node::class.qualifiedName}, but declared type is $nodeType")
          }

          val nodeDefinition = nodeBuilder.getNodeDefinition(nodeType)
          var node: Node? = null
          for(gameObject in sender.space.objects.all) {
            logger.trace { "Trying to build @JoinAll node $nodeDefinition for $gameObject" }

            node = nodeBuilder.tryBuild(nodeDefinition, gameObject.models.values.map { it.provide(gameObject, sender) }.toSet())
            if(node != null) {
              node.init(sender, gameObject)
              logger.trace { "Built @JoinAll node $node" }
              break
            }
          }

          if(node == null) {
            if(mandatory) {
              throw IllegalArgumentException("Failed to build @JoinAll node $nodeDefinition")
            } else {
              logger.trace { "Failed to build @JoinAll node $nodeDefinition" }
              continue@method
            }
          }

          args[parameter] = node
        }

        val instance = system.createInstance()
        args[instanceParameter] = instance

        logger.trace { "Invoking ${system.qualifiedName}::${method.name} with ${args.mapKeys { (parameter, _) -> "(${parameter.name}: ${parameter.type})" }}" }
        if(method.isSuspend) {
          if(outOfOrder) {
            sender.socket.launch {
              method.callSuspendBy(args)
            }
          } else {
            val timeout = 5.seconds
            val job = scope.launch {
              delay(timeout)
              logger.error { "Method ${system.qualifiedName}::${method.name} timed out ($timeout) while processing $event" }
              logger.error { "Possible deadlock detected, try marking event handler with @${OutOfOrderExecution::class.simpleName} to not suspend incoming command queue" }
            }

            try {
              method.callSuspendBy(args)
              logger.trace { "Method ${system.qualifiedName}::${method.name} processed $event" }
            } finally {
              job.cancelAndJoin()
            }
          }
        } else {
          method.callBy(args)
        }
      }
    }
  }
}
