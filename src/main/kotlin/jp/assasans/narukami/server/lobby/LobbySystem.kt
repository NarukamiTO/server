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

package jp.assasans.narukami.server.lobby

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battleselect.BattleSelectModelCC
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.lobby.communication.ChatNode
import jp.assasans.narukami.server.lobby.user.RankLoaderModelCC
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull

data class LobbyNode(
  val lobbyLayoutNotify: LobbyLayoutNotifyModelCC,
) : Node()

data class UserNode(
  val username: UsernameComponent,
  val score: ScoreComponent,
  val crystals: CrystalsComponent,
) : Node()

data class UsernameComponent(val username: String) : IComponent
data class ScoreComponent(val score: Int) : IComponent
data class CrystalsComponent(val crystals: Int) : IComponent

class LobbySystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @OutOfOrderExecution
  suspend fun channelAdded(
    event: ChannelAddedEvent,
    dispatcher: DispatcherNode,
    @JoinAll gpuDetector: SingleNode<GPUDetectorModelCC>,
    @JoinAll lobby: LobbyNode,
    @JoinAll chat: ChatNode,
    @JoinAll rankLoader: SingleNode<RankLoaderModelCC>,
    @JoinAll battleSelect: SingleNode<BattleSelectModelCC>,
  ) {
    logger.info { "Channel added: $event" }
    logger.info { lobby.gameObject.adapt(lobby.context) }

    // User object is created in [LoginSystem#login]
    val userObject = dispatcher.context.requireSpaceChannel.sessionNotNull.userNotNull

    // The order of loading objects is important, user object must be loaded
    // before lobby object, otherwise user properties will not load on the client.
    // Rank loader object is a dependency of user object, so it must be loaded first.
    //
    // By loading [ReconnectModel] with [GPUDetectorModel], we start the hardware detection
    // process on the client. After that, the client loads either software.swf or hardware.swf,
    // followed by game.swf, which contains garage and battles.
    // Client notifies server when loading is complete in [onGpuDetectionComplete].
    DispatcherLoadObjectsManagedEvent(
      rankLoader.gameObject,
      userObject,
      lobby.gameObject,
      chat.gameObject,
      battleSelect.gameObject,
    ).schedule(dispatcher).await()

    // TODO: NodeAddedEvent is not yet automatically scheduled
    NodeAddedEvent().schedule(chat)
    NodeAddedEvent().schedule(battleSelect)
  }

  @OnEventFire
  @Mandatory
  fun onGpuDetectionComplete(
    event: GPUDetectorModelDetectionGpuCompletedEvent,
    lobby: LobbyNode,
  ) {
    // Once entrance object is unloaded (or entrance space channel is closed),
    // loading screen automatically appears on the client. This event hides it.
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)
  }

  @OnEventFire
  @Mandatory
  fun showMatchmaking(
    event: LobbyLayoutModelShowMatchmakingEvent,
    lobby: LobbyNode,
  ) {
    logger.warn { "Show matchmaking is not implemented" }
    LobbyLayoutNotifyModelBeginLayoutSwitchEvent(LayoutState.MATCHMAKING).schedule(lobby)
    LobbyLayoutNotifyModelEndLayoutSwitchEvent(LayoutState.MATCHMAKING, LayoutState.BATTLE_SELECT).schedule(lobby)
  }

  @OnEventFire
  @Mandatory
  fun showGarage(
    event: LobbyLayoutModelShowGarageEvent,
    lobby: LobbyNode,
  ) {
    logger.warn { "Show garage is not implemented" }
    LobbyLayoutNotifyModelBeginLayoutSwitchEvent(LayoutState.GARAGE).schedule(lobby)
    LobbyLayoutNotifyModelEndLayoutSwitchEvent(LayoutState.GARAGE, LayoutState.BATTLE_SELECT).schedule(lobby)
  }

  @OnEventFire
  @Mandatory
  fun showGarage(
    event: LobbyLayoutModelShowBattleSelectEvent,
    lobby: LobbyNode,
  ) {
    logger.warn { "Show battle select is not implemented" }
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)
  }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun sessionLogout(
    event: SessionLogoutEvent,
    // TODO: Session node?
    dispatcher: DispatcherNode,
  ) {
    val session = dispatcher.context.requireSpaceChannel.sessionNotNull
    logger.info { "Logout for $session" }

    // TODO: This repeats the code from [Session#close], but without closing the control channel
    val user = session.user
    if(user != null) {
      val lobbyChannel = checkNotNull(session.spaces.get(Space.stableId("lobby"))) {
        "Lobby channel not found for $this"
      }
      lobbyChannel.space.objects.remove(user)
      session.user = null
    }

    // To logout, we need to close all spaces and reopen the entrance space
    for(channel in session.spaces.all) {
      channel.close()
      session.spaces.remove(channel)
    }

    // Same as the bootstrapping code in [ControlChannel]
    DispatcherOpenSpaceEvent(Space.stableId("entrance")).schedule(dispatcher).await()

    logger.info { "Reopened entrance space for $session" }
  }
}

class SessionLogoutEvent : IEvent
