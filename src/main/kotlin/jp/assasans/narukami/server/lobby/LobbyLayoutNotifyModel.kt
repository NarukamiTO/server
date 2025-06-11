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

import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.IProtocolEnum
import jp.assasans.narukami.server.protocol.ProtocolEnum
import jp.assasans.narukami.server.protocol.ProtocolEvent
import jp.assasans.narukami.server.protocol.ProtocolModel

@ProtocolModel(6363628754704336051)
class LobbyLayoutNotifyModelCC : IModelConstructor

@ProtocolEvent(7772769798498565531)
data class LobbyLayoutNotifyModelBeginLayoutSwitchEvent(
  val state: LayoutState
) : IClientEvent

@ProtocolEvent(2265286421664457062)
class LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent : IClientEvent

@ProtocolEvent(9114987333090113769)
data class LobbyLayoutNotifyModelEndLayoutSwitchEvent(
  val origin: LayoutState,
  val state: LayoutState
) : IClientEvent

@ProtocolEnum
enum class LayoutState(override val value: Int) : IProtocolEnum<Int> {
  MATCHMAKING(0),
  BATTLE_SELECT(1),
  GARAGE(2),
  BATTLE(3),
  RELOAD_SPACE(4),
  CLAN(5),
}
