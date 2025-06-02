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

package jp.assasans.narukami.server.lobby.user

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.lobby.UserNode

data class NotifierNode(
  val userNotifier: UserNotifierModelCC,
  val uidNotifier: UidNotifierModelCC,
  val rankNotifier: RankNotifierModelCC,
  val onlineNotifier: OnlineNotifierModelCC,
) : Node()

class UserNotifierSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  fun subscribe(event: UserNotifierModelSubscribeEvent, notifier: NotifierNode, user: UserNode) {
    UidNotifierModelSetUidEvent(UidNotifierModelCC("Subscribe_${event.userId}", event.userId)).schedule(notifier)
    RankNotifierModelSetRankEvent(RankNotifierModelCC(1, event.userId)).schedule(notifier)
    UserNotifierModelSetOnlineEvent(OnlineNotifierData(true, 1, 0, event.userId)).schedule(notifier)
  }
}
