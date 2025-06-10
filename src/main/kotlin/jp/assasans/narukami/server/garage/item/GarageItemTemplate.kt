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
import jp.assasans.narukami.server.battlefield.tank.hull.asModel
import jp.assasans.narukami.server.battlefield.tank.paint.AnimatedColoringComponent
import jp.assasans.narukami.server.battlefield.tank.paint.ColoringModelCC
import jp.assasans.narukami.server.battlefield.tank.paint.StaticColoringComponent
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.garage.ItemCategoryEnum
import jp.assasans.narukami.server.garage.toViewCategory
import jp.assasans.narukami.server.res.*

abstract class GarageItemMounterTemplate : TemplateV2() {
  open fun create(id: Long, item: IGameObject, preview: Boolean = false): IGameObject = gameObject(id).apply {
    addModel(Item3DModelCC(mounted = !preview))
    addModel(DetachModelCC())
  }
}

data class Object3DComponent(val resource: Resource<Object3DRes, Eager>) : IComponent

object HullGarageItemMounterTemplate : GarageItemMounterTemplate() {
  override fun create(id: Long, item: IGameObject, preview: Boolean): IGameObject = super.create(id, item, preview).apply {
    require(item.getComponent<ItemCategoryComponent>().category == ItemCategoryEnum.ARMOR) {
      "HullGarageItemMounterTemplate can only be used for items with ItemCategoryEnum.ARMOR"
    }
    addModel(ItemCategoryModelCC(category = ItemCategoryEnum.ARMOR))
    addModel(ClosureModelProvider {
      item.getComponent<Object3DComponent>().resource.asModel()
    })
  }
}

object WeaponGarageItemMounterTemplate : GarageItemMounterTemplate() {
  override fun create(id: Long, item: IGameObject, preview: Boolean): IGameObject = super.create(id, item, preview).apply {
    require(item.getComponent<ItemCategoryComponent>().category == ItemCategoryEnum.WEAPON) {
      "WeaponGarageItemMounterTemplate can only be used for items with ItemCategoryEnum.WEAPON"
    }
    addModel(ItemCategoryModelCC(category = ItemCategoryEnum.WEAPON))
    addModel(ClosureModelProvider {
      item.getComponent<Object3DComponent>().resource.asModel()
    })
  }
}

object PaintGarageItemMounterTemplate : GarageItemMounterTemplate() {
  override fun create(id: Long, item: IGameObject, preview: Boolean): IGameObject = super.create(id, item, preview).apply {
    require(item.getComponent<ItemCategoryComponent>().category == ItemCategoryEnum.PAINT) {
      "PaintGarageItemMounterTemplate can only be used for items with ItemCategoryEnum.PAINT"
    }
    addModel(ItemCategoryModelCC(category = ItemCategoryEnum.PAINT))
    addModel(ClosureModelProvider {
      val staticColoring = item.getComponentOrNull<StaticColoringComponent>()
      val animatedColoring = item.getComponentOrNull<AnimatedColoringComponent>()
      require(staticColoring != null || animatedColoring != null) { "At least one of StaticColoringComponent or AnimatedColoringComponent must be present" }
      require(staticColoring == null || animatedColoring == null) { "Cannot have both StaticColoringComponent and AnimatedColoringComponent" }

      ColoringModelCC(
        animatedColoring = animatedColoring?.resource,
        coloring = staticColoring?.resource
      )
    })
  }
}

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
