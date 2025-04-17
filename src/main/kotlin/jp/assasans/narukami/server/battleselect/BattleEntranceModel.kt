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

package jp.assasans.narukami.server.battleselect

import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.net.command.ProtocolEvent
import jp.assasans.narukami.server.net.command.ProtocolModel

@ProtocolModel(532750833650130316)
class BattleEntranceModelCC : IModelConstructor

@ProtocolEvent(6130398608824800551)
class BattleEntranceModelEnterToBattleFailedEvent : IClientEvent

@ProtocolEvent(8466121840412813207)
class BattleEntranceModelEquipmentNotMatchConstraintsEvent : IClientEvent

@ProtocolEvent(6292253993629897880)
class BattleEntranceModelFightFailedServerIsHaltingEvent : IClientEvent

@ProtocolEvent(7272171964896889145)
data class BattleEntranceModelFightEvent(val team: BattleTeam) : IServerEvent

@ProtocolEvent(4347865518183058614)
class BattleEntranceModelJoinAsSpectatorEvent : IServerEvent
