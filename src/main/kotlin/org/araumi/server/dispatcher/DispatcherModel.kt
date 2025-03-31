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

package org.araumi.server.dispatcher

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import org.araumi.server.core.*
import org.araumi.server.net.SpaceChannel
import org.araumi.server.net.command.ProtocolEvent
import org.araumi.server.res.Resource

@ProtocolEvent(3216143066888387731)
data class DispatcherModelLoadDependenciesEvent(
  val dependencies: ObjectsDependencies
) : IClientEvent

@ProtocolEvent(7640916300855664666)
data class DispatcherModelLoadObjectsDataEvent(
  val objectsData: ObjectsData
) : IClientEvent

@ProtocolEvent(1816792453857564692)
data class DispatcherModelDependenciesLoadedEvent(
  val callbackId: Int
) : IServerEvent

data class DispatcherModelLoadDependenciesManagedEvent(
  val classes: List<IGameClass>,
  val resources: List<Resource<*, *>>,
) : IEvent {
  private val logger = KotlinLogging.logger { }

  var callbackId: Int = 0
  val deferred: CompletableDeferred<Unit> = CompletableDeferred()

  suspend fun await() {
    logger.info { "Waiting for dependencies $callbackId to load..." }
    return deferred.await()
  }
}

data class DispatcherModelLoadObjectsManagedEvent(
  val objects: List<IGameObject<*>>,
) : IEvent {
  private val logger = KotlinLogging.logger { }

  var callbackId: Int = 0
  val deferred: CompletableDeferred<Unit> = CompletableDeferred()

  suspend fun await() {
    logger.info { "Waiting for objects $callbackId to load..." }
    return deferred.await()
  }
}

data class DispatcherModelOpenSpaceEvent(
  val id: Long
) {
  private val logger = KotlinLogging.logger { }

  val deferred: CompletableDeferred<SpaceChannel> = CompletableDeferred()

  suspend fun await(): SpaceChannel {
    logger.info { "Waiting for space channel $id to open..." }
    return deferred.await()
  }
}
