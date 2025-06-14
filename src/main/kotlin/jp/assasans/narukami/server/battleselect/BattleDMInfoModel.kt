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

package jp.assasans.narukami.server.battleselect

import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolEvent
import jp.assasans.narukami.server.protocol.ProtocolModel

@ProtocolModel(994751080759166914)
data class BattleDMInfoModelCC(
  val users: List<BattleInfoUser>,
) : IModelConstructor

@ProtocolEvent(5500506947295869799)
data class BattleDMInfoModelAddUserEvent(val infoUser: BattleInfoUser) : IClientEvent

@ProtocolEvent(3174409532068648254)
data class BattleDMInfoModelRemoveUserEvent(val userId: Long) : IClientEvent

@ProtocolEvent(6635862338174306891)
data class BattleDMInfoModelUpdateUserScoreEvent(val userId: Long, val kills: Int) : IClientEvent
