/*
 * Araumi TO - a server software reimplementation for a certain browser tank game.
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

package jp.assasans.narukami.server.battleselect

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode

data class MapInfoNode(
  val model: MapInfoModelCC,
) : Node()

data class BattleInfoNode(
  val battleInfo: BattleInfoModelCC,
  val battleParamInfo: BattleParamInfoModelCC,
  val battleEntrance: BattleEntranceModelCC,
) : Node()

class BattleSelectSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @OutOfOrderExecution
  suspend fun battleSelectAdded(
    event: NodeAddedEvent,
    battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll dispatcher: DispatcherNode,
    @JoinAll mapInfo: MapInfoNode,
    @JoinAll battleInfo: BattleInfoNode,
    @JoinAll battleCreate: SingleNode<BattleCreateModelCC>,
  ) {
    // The order of loading objects is important, map info objects must be loaded
    // before battle create object, otherwise the client won't see any maps in battle create.
    DispatcherLoadObjectsManagedEvent(listOf(mapInfo.gameObject, battleCreate.gameObject)).schedule(dispatcher).await()
    logger.info { "Loaded map objects" }

    DispatcherLoadObjectsManagedEvent(listOf(battleInfo.gameObject)).schedule(dispatcher).await()
    logger.info { "Loaded battle objects" }

    // Update battle list on the client
    BattleSelectModelBattleItemsPacketJoinSuccessEvent().schedule(battleSelect)
  }

  @OnEventFire
  @Mandatory
  fun fight(event: BattleEntranceModelFightEvent, battleInfo: BattleInfoNode) {
    BattleEntranceModelEquipmentNotMatchConstraintsEvent().schedule(battleInfo)
  }
}
