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

import jp.assasans.narukami.server.core.ClosureModelProvider
import jp.assasans.narukami.server.core.IComponent
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.core.getComponent

data class ModificationComponent(
  val group: Long,
  val modification: Int,
) : IComponent

data class GaragePropertiesComponent(
  val properties: List<GaragePropertyParams>,
) : IComponent

abstract class ModificationGarageItemTemplate : GarageItemTemplate() {
  override fun instantiate(id: Long) = super.instantiate(id).apply {
    addModel(ClosureModelProvider {
      ModificationModelCC(
        baseItemId = it.getComponent<ModificationComponent>().group,
        modificationIndex = it.getComponent<ModificationComponent>().modification.toByte()
      )
    })
    addModel(DiscountForUpgradeModelCC())
    addModel(ClosureModelProvider {
      UpgradeableParamsConstructorModelCC(
        currentLevel = 0,
        remainingTimeInMS = 0,
        speedUpDiscount = 0,
        timeDiscount = 0,
        upgradeDiscount = 0,
        itemData = UpgradeParamsData(
          finalUpgradePrice = 0,
          initialUpgradePrice = 0,
          speedUpCoeff = 0.0,
          upgradeLevelsCount = 0,
          upgradeTimeCoeff = 0.0,
          properties = it.getComponent<GaragePropertiesComponent>().properties,
        ),
      )
    })
  }
}

object HullGarageItemTemplate : ModificationGarageItemTemplate() {
  override fun instantiate(id: Long) = super.instantiate(id)
}

object WeaponGarageItemTemplate : ModificationGarageItemTemplate() {
  override fun instantiate(id: Long) = super.instantiate(id)
}

object PaintGarageItemTemplate : GarageItemTemplate() {
  override fun instantiate(id: Long) = super.instantiate(id).apply {
    addModel(ItemFittingModelCC())
    addModel(
      ItemPropertiesModelCC(
        properties = listOf(
          ItemGaragePropertyData(ItemGarageProperty.RAILGUN_RESISTANCE, "42%"),
          ItemGaragePropertyData(ItemGarageProperty.SMOKY_RESISTANCE, "21%"),
          ItemGaragePropertyData(ItemGarageProperty.TWINS_RESISTANCE, "12%"),
        ),
      )
    )
  }
}
