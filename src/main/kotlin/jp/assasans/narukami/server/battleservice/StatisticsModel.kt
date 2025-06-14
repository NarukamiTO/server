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

package jp.assasans.narukami.server.battleservice

import jp.assasans.narukami.server.battleselect.BattleLimits
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolModel

@ProtocolModel(2085529474881905981)
data class StatisticsModelCC(
  val battleName: String?,
  val equipmentConstraintsMode: String?,
  val fund: Int,
  val limits: BattleLimits,
  val mapName: String,
  /**
   * Controls whether the battle is a matchmaking battle.
   */
  val matchBattle: Boolean,
  val maxPeopleCount: Int,
  val modeName: String,
  val parkourMode: Boolean,
  val running: Boolean,
  val spectator: Boolean,
  val suspiciousUserIds: List<Long>,
  val timeLeft: Int,
  /**
   * Controls whether the fund is displayed on the client.
   * [matchBattle] must be set to `false` for fund to be displayed.
   */
  val valuableRound: Boolean,
) : IModelConstructor
