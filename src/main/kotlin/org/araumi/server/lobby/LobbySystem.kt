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

package org.araumi.server.lobby

import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.core.*
import org.araumi.server.core.impl.TemplatedGameClass
import org.araumi.server.dispatcher.DispatcherLoadObjectsManagedEvent
import org.araumi.server.dispatcher.DispatcherNode

data class LobbyNode(
  val lobbyLayoutNotify: LobbyLayoutNotifyModelCC
) : Node()

class LobbySystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  suspend fun channelAdded(event: ChannelAddedEvent, dispatcher: DispatcherNode, @JoinAll lobby: LobbyNode) {
    logger.info { "Channel added: $event" }

    val lobbyObject = lobby.gameObject as IGameObject<TemplatedGameClass<LobbyTemplate>>
    logger.info { lobbyObject.adapt() }

    DispatcherLoadObjectsManagedEvent(listOf(lobbyObject)).schedule(dispatcher).await()

    // Once entrance object is unloaded (or entrance space channel is closed),
    // loading screen automatically appears on the client. This event hides it.
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)
  }
}
