/*
 * Araumi TO - a server software reimplementation for a certain browser tank game.
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

package org.araumi.server.core.impl

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.araumi.server.core.*
import org.araumi.server.dispatcher.DispatcherSystem
import org.araumi.server.entrance.LoginSystem
import org.araumi.server.extensions.kotlinClass
import org.araumi.server.net.SpaceChannel

class EventScheduler {
  private val logger = KotlinLogging.logger { }

  fun process(event: IEvent, sender: SpaceChannel, gameObject: IGameObject<*>) {
    if(event is IClientEvent) {
      sender.sendBatched {
        event.attach(gameObject).enqueue()
      }
    } else {
      processServerEvent(event, sender, gameObject)
    }
  }

  fun processServerEvent(event: IEvent, sender: SpaceChannel, gameObject: IGameObject<*>) {
    logger.debug { "Processing server event: $event" }

    val systems = listOf(
      DispatcherSystem::class,
      LoginSystem::class,
    )

    for(system in systems) {
      val methods = system.declaredFunctions
      for(method in methods) {
        if(!method.hasAnnotation<OnEventFire>()) {
          logger.debug { "Method $method is not annotated with @OnEventFire" }
          continue
        }

        if(method.parameters.none { it.kind == KParameter.Kind.INSTANCE }) {
          throw IllegalArgumentException("Method $method is not an instance method")
        }

        val parameters = method.valueParameters

        val eventClass = parameters[0].type.kotlinClass
        if(eventClass != event::class) {
          logger.debug { "Method $method expects event of type ${eventClass.qualifiedName}, but received event is of type ${event::class.qualifiedName}" }
          continue
        }

        @Suppress("UNCHECKED_CAST")
        val nodeClass = parameters[1].type.kotlinClass as KClass<out Node>
        if(!nodeClass.isSubclassOf(Node::class)) {
          throw IllegalArgumentException("Method $method expects second parameter to be ${Node::class.qualifiedName}, but declared type is ${nodeClass.qualifiedName}")
        }

        val nodeBuilder = NodeBuilder()
        val nodeDefinition = nodeBuilder.getNodeDefinition(nodeClass)

        logger.info { "Trying to build node $nodeDefinition" }
        val node = nodeBuilder.tryBuild(nodeDefinition, gameObject.models.values.toSet())
                   ?: throw IllegalArgumentException("Failed to build node $nodeDefinition")
        node.init(sender, gameObject)
        logger.info { "Built node $node" }

        val instance = system.createInstance()

        logger.info { "Invoking ${system.qualifiedName}::${method.name} with event $event and node $node" }
        if(method.isSuspend) {
          GlobalScope.launch {
            method.callSuspend(instance, event, node)
          }
        } else {
          method.call(instance, event, node)
        }
      }
    }
  }
}
