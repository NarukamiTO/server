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

import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.net.command.ProtocolEvent
import jp.assasans.narukami.server.net.command.ProtocolModel

@ProtocolModel(5385894712778438726)
class BattleChatModelCC : IModelConstructor

@ProtocolEvent(5446292333752834689)
data class BattleChatModelAddMessageEvent(
  val userId: Long,
  val message: String,
  val type: BattleTeam,
) : IClientEvent

@ProtocolEvent(5628810758806816321)
data class BattleChatModelAddSpectatorTeamMessageEvent(
  val uid: String,
  val message: String,
) : IClientEvent

@ProtocolEvent(909632825295029806)
data class BattleChatModelAddSystemMessageEvent(
  val message: String,
) : IClientEvent

@ProtocolEvent(2321967673566621796)
data class BattleChatModelAddTeamMessageEvent(
  val userId: Long,
  val message: String,
  val type: BattleTeam,
) : IClientEvent

@ProtocolEvent(684534556503643918)
data class BattleChatModelUpdateTeamHeaderEvent(
  val header: String,
) : IClientEvent

@ProtocolEvent(1943137779427586084)
data class BattleChatModelSendMessageEvent(
  val message: String,
  val teamOnly: Boolean,
) : IServerEvent
