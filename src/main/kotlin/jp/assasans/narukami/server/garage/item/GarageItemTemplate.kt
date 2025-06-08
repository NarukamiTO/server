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

import kotlin.time.Duration.Companion.days
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.garage.ItemCategoryEnum
import jp.assasans.narukami.server.garage.toViewCategory
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.Resource

class GarageItemComponent : IComponent
data class NameComponent(val name: String) : IComponent
data class DescriptionComponent(val description: String) : IComponent
data class ItemPreviewComponent(val resource: Resource<ImageRes, Lazy>) : IComponent
data class MinRankComponent(val minRank: Int) : IComponent
data class MaxRankComponent(val maxRank: Int) : IComponent
data class PositionComponent(val position: Int) : IComponent
data class ItemCategoryComponent(val category: ItemCategoryEnum) : IComponent
class BuyableComponent : IComponent
data class PriceComponent(val price: Int) : IComponent
data class DiscountComponent(val discount: Float) : IComponent

class CompositeModificationGarageItemComponent(
  val modifications: Map<Int, Set<IComponent>>
) : IComponent

abstract class GarageItemTemplate : PersistentTemplateV2() {
  override fun instantiate(id: Long) = gameObject(id).apply {
    addModel(ClosureModelProvider {
      DescriptionModelCC(
        name = it.getComponent<NameComponent>().name,
        description = it.getComponentOrNull<DescriptionComponent>()?.description ?: "",
      )
    })
    addModel(ClosureModelProvider {
      ItemModelCC(
        minRank = it.getComponentOrNull<MinRankComponent>()?.minRank ?: 1,
        maxRank = it.getComponentOrNull<MaxRankComponent>()?.maxRank ?: 31,
        position = it.getComponentOrNull<PositionComponent>()?.position ?: 0,
        preview = it.getComponent<ItemPreviewComponent>().resource,
      )
    })
    addModel(ClosureModelProvider {
      ItemCategoryModelCC(category = it.getComponent<ItemCategoryComponent>().category)
    })
    addModel(ClosureModelProvider {
      ItemViewCategoryModelCC(category = it.getComponent<ItemCategoryComponent>().category.toViewCategory())
    })
    addModel(ClosureModelProvider {
      BuyableModelCC(
        buyable = it.hasComponent<BuyableComponent>(),
        priceWithoutDiscount = it.getComponent<PriceComponent>().price,
      )
    })
    addModel(DiscountCollectorModelCC())
    // TODO: DiscountModel should not be added if DiscountComponent is not present
    addModel(ClosureModelProvider {
      DiscountModelCC(
        discount = it.getComponentOrNull<DiscountComponent>()?.discount ?: 0f,
        timeLeftInSeconds = 1.days.inWholeSeconds.toInt(),
        timeToStartInSeconds = 0,
      )
    })
    addModel(
      TimePeriodModelCC(
        isEnabled = true,
        isTimeless = true,
        timeLeftInSeconds = 0,
        timeToStartInSeconds = 0,
      )
    )
    addModel(PremiumItemModelCC(premiumItem = false))
    addModel(DataOwnerModelCC(dataOwnerId = 0))
  }
}
