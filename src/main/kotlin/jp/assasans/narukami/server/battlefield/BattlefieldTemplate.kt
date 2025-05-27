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

package jp.assasans.narukami.server.battlefield

import jp.assasans.narukami.server.battleservice.StatisticsDMModel
import jp.assasans.narukami.server.battleservice.StatisticsModelCC
import jp.assasans.narukami.server.core.IModelProvider
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.net.command.ProtocolClass

@ProtocolClass(42)
data class BattlefieldTemplate(
  val battlefield: BattlefieldModelCC,
  val battlefieldBonuses: BattlefieldBonusesModelCC,
  val battleFacilities: BattleFacilitiesModelCC,
  val battleChat: BattleChatModelCC,
  // Absence of [StatisticsModel] causes an unhelpful error, pointing to [TankModel#registerUser].
  val statistics: StatisticsModelCC,
  val statisticsDM: IModelProvider<StatisticsDMModel>,
  // Absence of [InventoryModel] causes an unhelpful error, pointing to [DroneIndicatorModel#updateBatteryIndicator].
  val inventory: InventoryModelCC,
  // val inventorySfx: InventorySfxModelCC,
  // val continueBattle: ContinueBattleModelCC,
  val battleDM: BattleDMModelCC,
) : ITemplate
