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
import jp.assasans.narukami.server.battlefield.UserGroupComponent
import jp.assasans.narukami.server.battleselect.BattleInfoNodeV2
import jp.assasans.narukami.server.battleselect.BattleSelectNodeV2
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.dispatcher.DispatcherUnloadObjectsManagedEvent
import jp.assasans.narukami.server.entrance.DispatcherNodeV2
import jp.assasans.narukami.server.lobby.communication.ChatNodeV2
import jp.assasans.narukami.server.lobby.user.RankLoaderTemplate
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull

data class LobbyNode(
  val lobbyLayoutNotify: LobbyLayoutNotifyModelCC,
) : Node()

@MatchTemplate(LobbyTemplate::class)
class LobbyNodeV2 : NodeV2()

@MatchTemplate(RankLoaderTemplate::class)
class RankLoaderNodeV2 : NodeV2()

data class UserNode(
  val userGroup: UserGroupComponent,
  val username: UsernameComponent,
  val score: ScoreComponent,
  val crystals: CrystalsComponent,
) : Node()

data class UsernameComponent(val username: String) : IComponent
data class ScoreComponent(val score: Int) : IComponent
data class CrystalsComponent(val crystals: Int) : IComponent

class LobbySystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun channelAdded(
    context: IModelContext,
    event: ChannelAddedEvent,
    dispatcher: DispatcherNodeV2,
    @Optional @JoinAll @AllowUnloaded lobby: LobbyNodeV2,
    @JoinAll @AllowUnloaded chat: ChatNodeV2,
    @JoinAll @AllowUnloaded rankLoader: RankLoaderNodeV2,
    @JoinAll @AllowUnloaded battleSelect: BattleSelectNodeV2,
  ) = context {
    // User object is created in [LoginSystem#login]
    val userObject = context.requireSpaceChannel.sessionNotNull.userNotNull

    // The order of loading objects is important, user object must be loaded
    // before lobby object, otherwise user properties will not load on the client.
    // Rank loader object is a dependency of user object, so it must be loaded first.
    //
    // By loading [ReconnectModel] with [GPUDetectorModel] (both in lobby object), we start
    // the hardware detection process on the client. After that, the client loads either
    // software.swf or hardware.swf, followed by game.swf, which contains garage and battles.
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

  @OnEventFireV2
  fun onGpuDetectionComplete(
    context: IModelContext,
    event: GPUDetectorModelDetectionGpuCompletedEvent,
    lobby: LobbyNodeV2,
  ) = context {
    // Once entrance object is unloaded (or entrance space channel is closed),
    // loading screen automatically appears on the client. This event hides it.
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)
  }

  @OnEventFireV2
  fun showMatchmaking(
    context: IModelContext,
    event: LobbyLayoutModelShowMatchmakingEvent,
    lobby: LobbyNodeV2,
  ) = context {
    logger.warn { "Show matchmaking is not implemented" }
    LobbyLayoutNotifyModelBeginLayoutSwitchEvent(LayoutState.MATCHMAKING).schedule(lobby)
    LobbyLayoutNotifyModelEndLayoutSwitchEvent(LayoutState.MATCHMAKING, LayoutState.BATTLE_SELECT).schedule(lobby)
  }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun showGarage(
    context: IModelContext,
    event: LobbyLayoutModelShowGarageEvent,
    lobby: LobbyNodeV2,
    @JoinAll dispatcher: DispatcherNodeV2,
    @JoinAll battleSelect: BattleSelectNodeV2,
    @JoinAll battles: List<BattleInfoNodeV2>,
  ) = context {
    LobbyLayoutNotifyModelBeginLayoutSwitchEvent(LayoutState.GARAGE).schedule(lobby)
    LobbyLayoutNotifyModelEndLayoutSwitchEvent(LayoutState.GARAGE, LayoutState.GARAGE).schedule(lobby)

    DispatcherUnloadObjectsManagedEvent(
      listOf(
        battleSelect.gameObject
      ) + battles.gameObjects
    ).schedule(dispatcher)

    DispatcherOpenSpaceEvent(Space.stableId("garage")).schedule(dispatcher).await()
  }

  @OnEventFireV2
  fun showBattleSelect(
    context: IModelContext,
    event: LobbyLayoutModelShowBattleSelectEvent,
    lobby: LobbyNodeV2,
  ) = context {
    logger.warn { "Show battle select is not implemented" }
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)
  }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun sessionLogout(
    context: IModelContext,
    event: SessionLogoutEvent,
    // TODO: Session node?
    dispatcher: DispatcherNodeV2,
  ) = context {
    val session = context.requireSpaceChannel.sessionNotNull
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
    }

    // Same as the bootstrapping code in [ControlChannel]
    DispatcherOpenSpaceEvent(Space.stableId("entrance")).schedule(dispatcher).await()

    logger.info { "Reopened entrance space for $session" }
  }
}

class SessionLogoutEvent : IEvent
