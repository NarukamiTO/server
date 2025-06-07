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
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.garage.item.GarageItemTemplate

data class GarageNode(
  val garage: GarageModelCC,
  val upgradeGarageItem: UpgradeGarageItemModelCC,
) : Node()

class GarageSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @OutOfOrderExecution
  suspend fun channelAdded(
    event: ChannelAddedEvent,
    dispatcher: DispatcherNode,
    @JoinAll @AllowUnloaded garage: GarageNode,
  ) {
    DispatcherLoadObjectsManagedEvent(garage.gameObject).schedule(dispatcher).await()

    val item = TransientGameObject.instantiate(
      id = TransientGameObject.transientId("Item:0"),
      parent = TemplatedGameClass.fromTemplate(GarageItemTemplate::class),
      template = GarageItemTemplate.Provider.create()
    )

    DispatcherLoadObjectsManagedEvent(
      item
    ).schedule(dispatcher).await()

    GarageModelInitMarketEvent(listOf(item)).schedule(garage)
    // Additionally, it starts garage preview rendering
    GarageModelInitDepotEvent(listOf()).schedule(garage)
  }
}
