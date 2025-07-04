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

package jp.assasans.narukami.server.battlefield.chat

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battlefield.BattleUserNodeV2
import jp.assasans.narukami.server.battlefield.BattlefieldTemplate
import jp.assasans.narukami.server.battlefield.UserGroupComponent
import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.lobby.communication.UserNodeV2
import jp.assasans.narukami.server.lobby.communication.remote

data class BattleDebugMessageEvent(
  val message: String,
) : IEvent

@MatchTemplate(BattlefieldTemplate::class)
class BattleChatNode : NodeV2()

class BattleChatSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun sendMessage(
    context: IModelContext,
    event: BattleChatModelSendMessageEvent,
    chat: BattleChatNode,
    // XXX: @AllowUnloaded because object is loaded in different space
    @AllowUnloaded user: UserNodeV2,
    // @AllowUnloaded because it is server-only object
    @JoinAll @JoinBy(UserGroupComponent::class) @AllowUnloaded battleUser: BattleUserNodeV2,
  ) = context {
    val chatShared = remote(chat)

    logger.debug { "Send message to battle chat: $event" }
    BattleChatModelAddMessageEvent(
      userId = user.userGroup.key,
      message = event.message,
      type = battleUser.team?.team ?: BattleTeam.NONE,
    ).schedule(chat, chatShared)
  }

  @OnEventFireV2
  fun debugMessage(
    context: IModelContext,
    event: BattleDebugMessageEvent,
    chat: BattleChatNode,
  ) = context {
    BattleChatModelAddSystemMessageEvent(message = event.message).schedule(chat)
  }
}
