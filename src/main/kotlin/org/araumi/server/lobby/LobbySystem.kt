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
import org.araumi.server.core.impl.TransientGameObject
import org.araumi.server.dispatcher.DispatcherLoadObjectsManagedEvent
import org.araumi.server.dispatcher.DispatcherNode
import org.araumi.server.lobby.user.*
import org.araumi.server.net.NettySocketClient
import org.araumi.server.net.sessionNotNull

data class LobbyNode(
  val lobbyLayoutNotify: LobbyLayoutNotifyModelCC
) : Node()

class LobbySystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  suspend fun channelAdded(event: ChannelAddedEvent, dispatcher: DispatcherNode, @JoinAll lobby: LobbyNode) {
    logger.info { "Channel added: $event" }

    val lobbyObject = lobby.gameObject as IGameObject<TemplatedGameClass<LobbyTemplate>>
    logger.info { lobbyObject.adapt(lobby.sender) }

    val userClass = TemplatedGameClass.fromTemplate(UserTemplate::class)
    val userObject = TransientGameObject.instantiate(
      30,
      userClass,
      UserTemplate(
        rankLoader = RankLoaderModelCC(
          ranks = listOf(
            RankInfo(index = 1, name = "Долбаеб 1"),
            RankInfo(index = 1, name = "Долбаеб 2"),
          )
        ),
        userProperties = ClosureModelProvider {
          val ip = (lobby.sender.sessionNotNull.controlChannel.socket as NettySocketClient).channel.remoteAddress()
          UserPropertiesModelCC(
            canUseGroup = false,
            crystals = 123,
            crystalsRating = 0,
            daysFromLastVisit = 0,
            daysFromRegistration = 0,
            gearScore = 0,
            goldsTakenRating = 0,
            hasSpectatorPermissions = false,
            id = 30,
            rank = 1,
            rankBounds = RankBounds(lowBound = 10, topBound = 20),
            registrationTimestamp = 10,
            score = 10,
            scoreRating = 10,
            uid = "AraumiTO:AGPLv3+ ($ip)",
            userProfileUrl = "",
            userRating = 0
          )
        },
        userNotifier = UserNotifierModelCC(currentUserId = 30),
        uidNotifier = UidNotifierModelCC(uid = "AraumiTO:AGPLv3+", userId = 30),
        rankNotifier = RankNotifierModelCC(rank = 1, userId = 30),
      )
    )

    // The order of loading objects is important, user object MUST be loaded before lobby object,
    // otherwise user properties will not load on the client.
    DispatcherLoadObjectsManagedEvent(listOf(userObject, lobbyObject)).schedule(dispatcher).await()

    // Once entrance object is unloaded (or entrance space channel is closed),
    // loading screen automatically appears on the client. This event hides it.
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)
  }
}
