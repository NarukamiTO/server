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
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.garage.item.*
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

data class GarageNode(
  val garage: GarageModelCC,
  val upgradeGarageItem: UpgradeGarageItemModelCC,
) : Node()

data class GarageItemNode(
  val garageItem: GarageItemComponent,
) : Node()

class GarageSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  @OnEventFire
  @OutOfOrderExecution
  suspend fun channelAdded(
    event: ChannelAddedEvent,
    dispatcher: DispatcherNode,
    @JoinAll @AllowUnloaded garage: GarageNode,
    @JoinAll @AllowUnloaded items: List<GarageItemNode>,
  ) {
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

    GarageModelInitMarketEvent(items.gameObjects).schedule(garage)
    // Additionally, it starts garage preview rendering
    GarageModelInitDepotEvent(listOf()).schedule(garage)
  }
}
