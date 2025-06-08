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

package jp.assasans.narukami.server.garage.item

import jp.assasans.narukami.server.battlefield.tank.ItemProperty
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.net.command.ProtocolStruct

@ProtocolModel(4743567393313674375)
data class UpgradeableParamsConstructorModelCC(
  val currentLevel: Int,
  val remainingTimeInMS: Int,
  val speedUpDiscount: Int,
  val timeDiscount: Int,
  val upgradeDiscount: Int,
  val itemData: UpgradeParamsData,
) : IModelConstructor

@ProtocolStruct
data class UpgradeParamsData(
  val finalUpgradePrice: Int,
  val initialUpgradePrice: Int,
  val speedUpCoeff: Double,
  val upgradeLevelsCount: Int,
  val upgradeTimeCoeff: Double,
  val properties: List<GaragePropertyParams>,
)

@ProtocolStruct
data class GaragePropertyParams(
  val property: ItemGarageProperty,
  val properties: List<PropertyData>,
  val precision: Int = 0,
  val visibleInInfo: Boolean = true,
)

@ProtocolStruct
data class PropertyData(
  val property: ItemProperty,
  val initialValue: Float,
  val finalValue: Float = initialValue,
)
