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

package jp.assasans.narukami.server.garage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.GameObjectIdSource
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherUnloadObjectsManagedEvent
import jp.assasans.narukami.server.entrance.DispatcherNodeV2
import jp.assasans.narukami.server.garage.item.*
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

@MatchTemplate(GarageTemplate::class)
class GarageNode : NodeV2()

data class GarageItemNode(
  val garageItem: GarageItemComponent,
) : NodeV2()

data class CompositeModificationGarageItemNode(
  val compositeModificationGarageItem: CompositeModificationGarageItemComponent,
) : NodeV2()

@MatchTemplate(GarageItemMounterTemplate::class)
class GarageItemMounterNode : NodeV2()

class GarageSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  @OnEventFireV2
  fun flattenCompositeItem(
    context: SpaceModelContext,
    event: NodeAddedEvent,
    @Optional compositeItem: CompositeModificationGarageItemNode,
  ) {
    val template = compositeItem.gameObject.template
    if(template !is PersistentTemplateV2) throw IllegalStateException("$template is not a persistent template")

    for((modification, components) in compositeItem.compositeModificationGarageItem.modifications) {
      val id = GameObjectIdSource.transientId("CompositeItem:${compositeItem.gameObject.id}:$modification")
      val gameObject = template.instantiate(id)
      gameObject.addComponent(GarageItemComponent())
      gameObject.addComponent(ModificationComponent(group = compositeItem.gameObject.id, modification = modification))
      gameObject.addAllComponents(compositeItem.gameObject.allComponents.values.filterNot { it is CompositeModificationGarageItemComponent })
      gameObject.addAllComponents(components)

      logger.info { "Flattened composite item '${gameObject.getComponent<NameComponent>().name} M$modification': $gameObject" }
      context.space.objects.add(gameObject)
    }
  }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun channelAdded(
    context: IModelContext,
    event: ChannelAddedEvent,
    dispatcher: DispatcherNodeV2,
    @Optional @JoinAll @AllowUnloaded garage: GarageNode,
    @JoinAll @AllowUnloaded items: List<GarageItemNode>,
  ) = context {
    DispatcherLoadObjectsManagedEvent(garage.gameObject).schedule(dispatcher).await()

    fun IGameObject.fakeData() {
      addComponent(GarageItemComponent())
      addComponent(NameComponent(name = "Test Item"))
      addComponent(DescriptionComponent(description = "This is a test item for the garage system. It has no real functionality."))
      addComponent(
        ItemPreviewComponent(
          resource = gameResourceRepository.get(
            "tank.hull.viking.preview",
            mapOf("gen" to "1.0", "modification" to "0"),
            ImageRes,
            Lazy
          )
        )
      )
      addComponent(MinRankComponent(minRank = 1))
      addComponent(MaxRankComponent(maxRank = 31))
      addComponent(PositionComponent(position = 0))
      addComponent(ItemCategoryComponent(category = ItemCategoryEnum.ARMOR))
      addComponent(BuyableComponent())
      addComponent(PriceComponent(price = 1000))
      addComponent(DiscountComponent(discount = 0.1f))
    }

    DispatcherLoadObjectsManagedEvent(
      items.gameObjects
    ).schedule(dispatcher).await()

    val ownedItems = items.gameObjects.filter {
      if(it.hasComponent<ModificationComponent>()) it.getComponent<ModificationComponent>().modification < 2
      else it.getComponent<NameComponent>().name == "Fracture"
    }

    val hullMounter = HullGarageItemMounterTemplate.create(
      id = GameObjectIdSource.transientId("Mounter:Hull"),
      item = ownedItems.filter { it.template is HullGarageItemTemplate }.random()
    )
    val weaponMounter = WeaponGarageItemMounterTemplate.create(
      id = GameObjectIdSource.transientId("Mounter:Weapon"),
      item = ownedItems.filter { it.template is WeaponGarageItemTemplate }.random()
    )
    val paintMounter = PaintGarageItemMounterTemplate.create(
      id = GameObjectIdSource.transientId("Mounter:Paint"),
      item = ownedItems.filter { it.template is PaintGarageItemTemplate }.random()
    )
    context.space.objects.add(hullMounter)
    context.space.objects.add(weaponMounter)
    context.space.objects.add(paintMounter)
    DispatcherLoadObjectsManagedEvent(
      hullMounter,
      weaponMounter,
      paintMounter,
    ).schedule(dispatcher).await()

    GarageModelInitMarketEvent(items.gameObjects - ownedItems).schedule(garage)
    // Additionally, it starts garage preview rendering
    GarageModelInitDepotEvent(ownedItems).schedule(garage)

    GarageModelSelectEvent(items.gameObjects.first()).schedule(garage)
    GarageModelUpdateMountedItemsEvent(ownedItems).schedule(garage)
  }

  @OnEventFireV2
  fun detachMounter(
    context: IModelContext,
    event: DetachModelDetachEvent,
    garageItemMounter: GarageItemMounterNode,
    @JoinAll dispatcher: DispatcherNodeV2,
  ) = context {
    context.space.objects.remove(garageItemMounter.gameObject)
    DispatcherUnloadObjectsManagedEvent(garageItemMounter.gameObject).schedule(dispatcher)

    logger.info { "Detached garage item mounter: $garageItemMounter.gameObject" }
  }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun mount(
    context: IModelContext,
    event: GarageModelItemMountedEvent,
    garage: GarageNode,
    @JoinAll dispatcher: DispatcherNodeV2,
  ) = context {
    val item = event.item.adapt<GarageItemNode>()
    val itemMounter = when(item.gameObject.getComponent<ItemCategoryComponent>().category) {
      ItemCategoryEnum.ARMOR  -> HullGarageItemMounterTemplate.create(GameObjectIdSource.transientId("Mounter:Hull"), item.gameObject)
      ItemCategoryEnum.WEAPON -> WeaponGarageItemMounterTemplate.create(GameObjectIdSource.transientId("Mounter:Weapon"), item.gameObject)
      ItemCategoryEnum.PAINT  -> PaintGarageItemMounterTemplate.create(GameObjectIdSource.transientId("Mounter:Paint"), item.gameObject)
      else                    -> throw IllegalArgumentException("Cannot mount item of category ${item.gameObject.getComponent<ItemCategoryComponent>().category}")
    }

    context.space.objects.add(itemMounter)
    DispatcherLoadObjectsManagedEvent(itemMounter).schedule(dispatcher).await()
    logger.info { "Mounted garage item: ${item.garageItem}" }
  }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun fit(
    context: IModelContext,
    event: ItemFittingModelFitEvent,
    item: GarageItemNode,
    @JoinAll dispatcher: DispatcherNodeV2,
  ) = context {
    val itemMounter = when(item.gameObject.getComponent<ItemCategoryComponent>().category) {
      ItemCategoryEnum.PAINT -> PaintGarageItemMounterTemplate.create(GameObjectIdSource.transientId("Mounter:Paint"), item.gameObject, preview = true)
      else                   -> throw IllegalArgumentException("Cannot fit item of category ${item.gameObject.getComponent<ItemCategoryComponent>().category}")
    }

    context.space.objects.add(itemMounter)
    DispatcherLoadObjectsManagedEvent(itemMounter).schedule(dispatcher).await()
    logger.info { "Fitted garage item: ${item.garageItem}" }
  }
}
