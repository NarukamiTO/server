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

import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.net.command.ProtocolEvent
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.Object3DRes
import jp.assasans.narukami.server.res.Resource
import jp.assasans.narukami.server.res.TextureRes

@ProtocolModel(7381961590546665610)
data class GarageModelCC(
  val cameraAltitude: Float,
  val cameraDistance: Float,
  val cameraFov: Float,
  val cameraPitch: Float,
  val garageBox: Resource<Object3DRes, Eager>,
  /**
   * Unused
   */
  val hideLinks: Boolean,
  val mountableCategories: List<ItemCategoryEnum>,
  val skyboxBackSide: Resource<TextureRes, Eager>,
  val skyboxBottomSide: Resource<TextureRes, Eager>,
  val skyboxFrontSide: Resource<TextureRes, Eager>,
  val skyboxLeftSide: Resource<TextureRes, Eager>,
  val skyboxRightSide: Resource<TextureRes, Eager>,
  val skyboxTopSide: Resource<TextureRes, Eager>,
) : IModelConstructor

@ProtocolEvent(3996477900528000389)
data class GarageModelInitDepotEvent(
  val itemsOnDepot: List<IGameObject>,
) : IClientEvent

@ProtocolEvent(5236393599344825589)
data class GarageModelInitMarketEvent(
  val itemsOnMarket: List<IGameObject>,
) : IClientEvent

@ProtocolEvent(3692495084100053385)
data class GarageModelInitMountedEvent(
  val mountedItems: List<IGameObject>,
) : IClientEvent

@ProtocolEvent(4008258395901188095)
data class GarageModelReloadGarageEvent(
  val message: String,
  val totalCrystals: Int,
) : IClientEvent

@ProtocolEvent(5056382137818666244)
data class GarageModelRemoveDepotItemEvent(
  val item: IGameObject,
) : IClientEvent

@ProtocolEvent(7185392055235128859)
data class GarageModelSelectEvent(
  val itemToSelect: IGameObject,
) : IClientEvent

@ProtocolEvent(1936068342294351641)
class GarageModelSelectFirstItemInDepotEvent : IClientEvent

@ProtocolEvent(4036211072904074490)
data class GarageModelShowCategoryEvent(
  val viewCategory: ItemViewCategoryEnum,
) : IClientEvent

@ProtocolEvent(4091887481184252855)
class GarageModelUnmountDroneEvent : IClientEvent

@ProtocolEvent(8615808087035604385)
data class GarageModelUpdateDepotItemEvent(
  val item: IGameObject,
  val count: Int,
) : IClientEvent

@ProtocolEvent(5304675338465343952)
data class GarageModelUpdateMountedItemsEvent(
  val mountedItems: List<IGameObject>,
) : IClientEvent

@ProtocolEvent(6828807833188447228)
data class GarageModelUpdateTemporaryItemEvent(
  val item: IGameObject,
  val remainingTimeSeconds: Int,
) : IClientEvent

@ProtocolEvent(4795277252675033851)
data class GarageModelItemBoughtEvent(
  val lightItem: IGameObject,
  val count: Int,
  val expectedPrice: Int,
) : IServerEvent

@ProtocolEvent(1079642233486876258)
data class GarageModelItemMountedEvent(
  val item: IGameObject,
) : IServerEvent

@ProtocolEvent(4518511464607698075)
data class GarageModelItemUnmountedEvent(
  val item: IGameObject,
) : IServerEvent

@ProtocolEvent(8771158958910681946)
data class GarageModelKitBoughtEvent(
  val item: IGameObject,
  val expectedPrice: Int,
) : IServerEvent

@ProtocolEvent(2185839955975110192)
class GarageModelReadyToReloadEvent : IServerEvent
