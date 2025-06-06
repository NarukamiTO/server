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
import jp.assasans.narukami.server.battlefield.BattleUserNode
import jp.assasans.narukami.server.battlefield.UserGroupComponent
import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.lobby.UserNode

data class BattleDebugMessageEvent(
  val message: String,
) : IEvent

class BattleChatSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  fun sendMessage(
    event: BattleChatModelSendMessageEvent,
    chat: SingleNode<BattleChatModelCC>,
    // XXX: @AllowUnloaded because object is loaded in different space
    @AllowUnloaded user: UserNode,
    // @AllowUnloaded because it is server-only object
    @JoinAll @JoinBy(UserGroupComponent::class) @AllowUnloaded battleUser: BattleUserNode,
    @PerChannel chatShared: List<SingleNode<BattleChatModelCC>>,
  ) {
    logger.debug { "Send message to battle chat: $event" }
    BattleChatModelAddMessageEvent(
      userId = user.gameObject.id,
      message = event.message,
      type = BattleTeam.NONE,
    ).schedule(chatShared)
  }

  @OnEventFire
  @Mandatory
  fun debugMessage(
    event: BattleDebugMessageEvent,
    chat: SingleNode<BattleChatModelCC>,
  ) {
    BattleChatModelAddSystemMessageEvent(message = event.message).schedule(chat)
  }
}
