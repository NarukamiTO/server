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

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.*
import jp.assasans.narukami.server.battleservice.StatisticsDMModel
import jp.assasans.narukami.server.battleservice.StatisticsModelCC
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherModelUnloadObjectsEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.lobby.communication.ChatNode
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.RemoteGameResourceRepository
import jp.assasans.narukami.server.res.SoundRes

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

  private val spaces: IRegistry<ISpace> by inject()
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  @OnEventFire
  @OnlySpaceContext
  fun mapLoaded(
    event: NodeAddedEvent,
    mapInfo: MapInfoNode,
    @JoinAll battleSelect: SingleNode<BattleSelectModelCC>,
  ) {
    val battleId = Space.freeId()

    val battleInfoClass = TemplatedGameClass.fromTemplate(DMBattleInfoTemplate::class)
    val battleInfoObject = TransientGameObject.instantiate(
      battleId,
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
              limits = BattleLimits(
                scoreLimit = 999,
                timeLimitInSec = (21.minutes + 12.seconds).inWholeSeconds.toInt()
              ),
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
    mapInfo.context.space.objects.add(battleInfoObject)

    spaces.add(Space(battleId).apply {
      objects.remove(rootObject)

      val battleMapObject = TransientGameObject.instantiate(
        id = TransientGameObject.freeId(),
        parent = TemplatedGameClass.fromTemplate(BattleMapTemplate::class),
        template = BattleMapTemplate.Provider.create()
      )
      // TODO: Should BattleMapTemplate just access components from map info object?
      battleMapObject.components.putAll(mapInfo.gameObject.components)
      objects.add(battleMapObject)

      val battlefieldObject = TransientGameObject.instantiate(
        id,
        parent = TemplatedGameClass.fromTemplate(BattlefieldTemplate::class),
        BattlefieldTemplate(
          battlefield = BattlefieldModelCC(
            active = true,
            battleId = id,
            battlefieldSounds = BattlefieldSounds(
              battleFinishSound = gameResourceRepository.get("battle.sound.finish", mapOf(), SoundRes, Eager),
              killSound = gameResourceRepository.get("tank.sound.destroy", mapOf(), SoundRes, Eager)
            ),
            colorTransformMultiplier = 0.0f,
            idleKickPeriodMsec = 0,
            map = battleMapObject,
            mineExplosionLighting = LightingSFXEntity(effects = listOf()),
            proBattle = true,
            range = Range(min = 1, max = 31),
            reArmorEnabled = false,
            respawnDuration = 2000,
            shadowMapCorrectionFactor = 0.0f,
            showAddressLink = false,
            spectator = false,
            withoutBonuses = false,
            withoutDrones = false,
            withoutSupplies = false
          ),
          battlefieldBonuses = BattlefieldBonusesModelCC(
            bonusFallSpeed = 0.0f,
            bonuses = listOf()
          ),
          battleFacilities = BattleFacilitiesModelCC(),
          battleChat = BattleChatModelCC(),
          statistics = StatisticsModelCC(
            battleName = null,
            equipmentConstraintsMode = null,
            fund = 42,
            limits = BattleLimits(
              scoreLimit = 999,
              timeLimitInSec = (21.minutes + 12.seconds).inWholeSeconds.toInt()
            ),
            mapName = "BattlefieldTemplate",
            matchBattle = false,
            maxPeopleCount = 0,
            modeName = "DM",
            parkourMode = false,
            running = true,
            spectator = false,
            suspiciousUserIds = listOf(),
            timeLeft = (21.minutes + 12.seconds).inWholeSeconds.toInt(),
            valuableRound = true
          ),
          statisticsDM = ClosureModelProvider {
            val battleUsers = space.objects.all
              .filter { it.components.contains(BattleUserComponent::class) }
              .map { it.makeNode<BattleUserNode>(this@ClosureModelProvider) }
            StatisticsDMModel(
              usersInfo = battleUsers.map { battleUser -> battleUser.asUserInfo(space.objects.all) }
            )
          },
          inventory = InventoryModelCC(ultimateEnabled = false),
          battleDM = BattleDMModelCC(),
        )
      )
      replaceRootObject(battlefieldObject)
    })
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
    DispatcherLoadObjectsManagedEvent(
      mapInfo.gameObjects + listOf(battleCreate.gameObject)
    ).schedule(dispatcher).await()
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

    DispatcherOpenSpaceEvent(battleInfo.gameObject.id).schedule(dispatcher).await()
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
