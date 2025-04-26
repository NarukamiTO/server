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

package jp.assasans.narukami.server.battleselect

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherModelUnloadObjectsEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.lobby.communication.ChatNode

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
  fun spaceCreated(
    event: SpaceCreatedEvent,
    dispatcher: DispatcherNode,
    @JoinAll battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll maps: List<MapInfoNode>,
  ) {
    for(mapInfo in maps) {
      val battleInfoClass = TemplatedGameClass.fromTemplate(DMBattleInfoTemplate::class)
      val battleInfoObject = TransientGameObject.instantiate(
        TransientGameObject.freeId(),
        battleInfoClass,
        DMBattleInfoTemplate(
          common = BattleInfoTemplate(
            battleInfo = BattleInfoModelCC(
              roundStarted = false,
              suspicionLevel = BattleSuspicionLevel.NONE,
              timeLeftInSec = 3600,
            ),
            battleParamInfo = BattleParamInfoModelCC(
              map = mapInfo.gameObject,
              params = BattleCreateParameters(
                autoBalance = false,
                battleMode = BattleMode.DM,
                clanBattle = false,
                dependentCooldownEnabled = false,
                equipmentConstraintsMode = null,
                friendlyFire = false,
                goldBoxesEnabled = false,
                limits = BattleLimits(scoreLimit = 0, timeLimitInSec = 0),
                mapId = mapInfo.gameObject.id,
                maxPeopleCount = 32,
                name = null,
                parkourMode = false,
                privateBattle = false,
                proBattle = true,
                rankRange = Range(min = 1, max = 31),
                reArmorEnabled = true,
                theme = MapTheme.SUMMER_NIGHT,
                ultimatesEnabled = false,
                uniqueUsersBattle = false,
                withoutBonuses = false,
                withoutDevices = false,
                withoutDrones = false,
                withoutSupplies = false,
                withoutUpgrades = false,
              )
            ),
            battleEntrance = BattleEntranceModelCC(),
          ),
          battleDMInfo = BattleDMInfoModelCC(
            users = listOf()
          )
        )
      )
      event.space.objects.add(battleInfoObject)
    }
  }

  @OnEventFire
  @OutOfOrderExecution
  suspend fun battleSelectAdded(
    event: NodeAddedEvent,
    battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll dispatcher: DispatcherNode,
    @JoinAll mapInfo: List<MapInfoNode>,
    @JoinAll battleInfo: List<BattleInfoNode>,
    @JoinAll battleCreate: SingleNode<BattleCreateModelCC>,
  ) {
    // The order of loading objects is important, map info objects must be loaded
    // before battle create object, otherwise the client won't see any maps in battle create.
    DispatcherLoadObjectsManagedEvent(mapInfo.gameObjects + listOf(battleCreate.gameObject)).schedule(dispatcher).await()
    logger.info { "Loaded map objects" }

    DispatcherLoadObjectsManagedEvent(battleInfo.gameObjects).schedule(dispatcher).await()
    logger.info { "Loaded battle objects" }

    // Update battle list on the client
    BattleSelectModelBattleItemsPacketJoinSuccessEvent().schedule(battleSelect)
  }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun fight(
    event: BattleEntranceModelFightEvent,
    battleInfo: BattleInfoNode,
    @JoinAll chat: ChatNode,
    @JoinAll battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll dispatcher: DispatcherNode,
  ) {
    DispatcherModelUnloadObjectsEvent(
      objects = listOf(chat.gameObject, battleSelect.gameObject)
    ).schedule(dispatcher)

    DispatcherOpenSpaceEvent(4242).schedule(dispatcher).await()
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrivateMapDataEntity(
  val proplibs: List<MapProplib>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MapProplib(
  val name: String,
  val id: Long,
  val version: Long,
  val namespaces: Map<String, String>,
)
