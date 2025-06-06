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

import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.*
import jp.assasans.narukami.server.battlefield.chat.BattleChatModelCC
import jp.assasans.narukami.server.battleservice.StatisticsDMModel
import jp.assasans.narukami.server.battleservice.StatisticsModelCC
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.dispatcher.DispatcherUnloadObjectsManagedEvent
import jp.assasans.narukami.server.extensions.roundToNearest
import jp.assasans.narukami.server.lobby.*
import jp.assasans.narukami.server.lobby.communication.ChatNode
import jp.assasans.narukami.server.lobby.user.UserTemplate
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull
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

data class BattleSpaceComponent(val space: ISpace) : IComponent
data class BattleInfoGroupComponent(override val reference: IGameObject) : IGroupComponent

data class AddBattleUserEvent(val battleUser: BattleUserNode) : IEvent
data class RemoveBattleUserEvent(val battleUser: BattleUserNode) : IEvent

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
        battleDMInfo = ClosureModelProvider {
          val battleUsers = it.getComponent<BattleSpaceComponent>()
            .space.objects.all
            .filterHasComponent<BattleUserComponent>()
            .map { it.adapt<BattleUserNode>() }
            .filter { it.team != null }
          BattleDMInfoModelCC(
            users = battleUsers.map { battleUser -> battleUser.asBattleInfoUser() }
          )
        }
      )
    )
    battleInfoObject.addComponent(BattleInfoGroupComponent(battleInfoObject))
    mapInfo.context.space.objects.add(battleInfoObject)

    spaces.add(Space(battleId).apply {
      battleInfoObject.addComponent(BattleSpaceComponent(this))

      objects.remove(rootObject)

      val battleMapObject = TransientGameObject.instantiate(
        id = TransientGameObject.transientId("Map:${mapInfo.gameObject.id}"),
        parent = TemplatedGameClass.fromTemplate(BattleMapTemplate::class),
        template = BattleMapTemplate.Provider.create()
      )
      // TODO: Should BattleMapTemplate just access components from map info object?
      battleMapObject.addAllComponents(mapInfo.gameObject.allComponents.values)
      battleMapObject.addComponent(BattleInfoGroupComponent(battleInfoObject))
      objects.add(battleMapObject)

      val battlefieldObject = TransientGameObject.instantiate(
        id,
        parent = TemplatedGameClass.fromTemplate(BattlefieldTemplate::class),
        BattlefieldTemplate(
          battlefield = ClosureModelProvider {
            // TODO: Design of model providers is broken
            val fakeUser = SingleNode(UserGroupComponent(requireSpaceChannel.sessionNotNull.userNotNull))
            fakeUser.init(this, fakeUser.node.reference)
            val battleUser = space.objects.all.findBy<BattleUserNode, UserGroupComponent>(fakeUser)

            BattlefieldModelCC(
              active = true,
              battleId = id,
              battlefieldSounds = BattlefieldSounds(
                battleFinishSound = gameResourceRepository.get("battle.sound.finish", mapOf(), SoundRes, Eager),
                killSound = gameResourceRepository.get("tank.sound.destroy", mapOf(), SoundRes, Eager)
              ),
              colorTransformMultiplier = 1.0f,
              idleKickPeriodMsec = 5.minutes.inWholeMilliseconds.toInt(),
              map = battleMapObject,
              mineExplosionLighting = LightingSFXEntity(
                effects = listOf(
                  LightingEffectEntity(
                    "explosion", listOf(
                      LightEffectItem(300f, 0f, "0xff0000", 0.5f, 0),
                      LightEffectItem(1f, 2f, "0xff0000", 0f, 500)
                    )
                  )
                )
              ),
              proBattle = true,
              range = Range(min = 1, max = 31),
              reArmorEnabled = false,
              respawnDuration = 2000,
              shadowMapCorrectionFactor = 0.0f,
              showAddressLink = false,
              spectator = battleUser.spectator != null,
              withoutBonuses = false,
              withoutDrones = false,
              withoutSupplies = false
            )
          },
          battlefieldBonuses = BattlefieldBonusesModelCC(
            bonusFallSpeed = 0.0f,
            bonuses = listOf()
          ),
          battleFacilities = BattleFacilitiesModelCC(),
          battleChat = BattleChatModelCC(),
          statistics = ClosureModelProvider {
            // TODO: Design of model providers is broken
            val fakeUser = SingleNode(UserGroupComponent(requireSpaceChannel.sessionNotNull.userNotNull))
            fakeUser.init(this, fakeUser.node.reference)
            val battleUser = space.objects.all.findBy<BattleUserNode, UserGroupComponent>(fakeUser)

            StatisticsModelCC(
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
              spectator = battleUser.spectator != null,
              suspiciousUserIds = listOf(),
              timeLeft = (21.minutes + 12.seconds).inWholeSeconds.toInt(),
              valuableRound = true
            )
          },
          statisticsDM = ClosureModelProvider {
            val tanks = space.objects.all.filter { it.hasComponent<TankLogicStateComponent>() }
            StatisticsDMModel(
              usersInfo = space.objects.all
                .filterHasComponent<BattleUserComponent>()
                .map { it.adapt<BattleUserNode>() }
                // Exclude battle users that have no tank object
                // TODO: Workaround, works for now
                .filter { battleUser -> tanks.any { it.getComponent<UserGroupComponent>() == battleUser.userGroup } }
                .map { it.asUserInfo(space.objects.all) }
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
  @Mandatory
  fun addBattleUser(
    event: AddBattleUserEvent,
    // @AllowUnloaded because object is unloaded for self
    // XXX: Unless the user is in battle list
    @AllowUnloaded battleInfo: BattleInfoNode,
    @PerChannel battleInfoShared: List<BattleInfoNode>,
  ) {
    logger.info { "Adding battle user ${event.battleUser.gameObject.id} to battle ${battleInfo.gameObject.id}, ${(battleInfoShared - battleInfo).size} shared" }
    BattleDMInfoModelAddUserEvent(event.battleUser.asBattleInfoUser()).schedule(battleInfoShared - battleInfo)
  }

  @OnEventFire
  @Mandatory
  fun removeBattleUser(
    event: RemoveBattleUserEvent,
    // @AllowUnloaded because object is unloaded for self
    // XXX: Unless the user is in battle list
    @AllowUnloaded battleInfo: BattleInfoNode,
    @PerChannel battleInfoShared: List<BattleInfoNode>,
  ) {
    logger.info { "Removing battle user ${event.battleUser.gameObject.id} from battle ${battleInfo.gameObject.id}, ${(battleInfoShared - battleInfo).size} shared" }
    BattleDMInfoModelRemoveUserEvent(event.battleUser.gameObject.id).schedule(battleInfoShared - battleInfo)
  }

  @OnEventFire
  @OutOfOrderExecution
  suspend fun battleSelectAdded(
    event: NodeAddedEvent,
    battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll dispatcher: DispatcherNode,
    @JoinAll @AllowUnloaded mapInfo: List<MapInfoNode>,
    @JoinAll @AllowUnloaded battleInfo: List<BattleInfoNode>,
    @JoinAll @AllowUnloaded battleCreate: SingleNode<BattleCreateModelCC>,
  ) {
    // The order of loading objects is important, map info objects must be loaded
    // before battle create object, otherwise the client won't see any maps in battle create.
    DispatcherLoadObjectsManagedEvent(
      mapInfo.gameObjects + listOf(battleCreate.gameObject)
    ).schedule(dispatcher).await()
    logger.debug { "Loaded map objects" }

    DispatcherLoadObjectsManagedEvent(battleInfo.gameObjects).schedule(dispatcher).await()
    logger.debug { "Loaded battle objects" }

    // Update battle list on the client
    BattleSelectModelBattleItemsPacketJoinSuccessEvent().schedule(battleSelect)
  }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun fight(
    event: BattleEntranceModelFightEvent,
    battleInfo: BattleInfoNode,
    @PerChannel battleInfoShared: List<BattleInfoNode>,
    user: UserNode,
    @JoinAll lobby: LobbyNode,
    @JoinAll chat: ChatNode,
    @JoinAll battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll battles: List<BattleInfoNode>,
    @JoinAll dispatcher: DispatcherNode,
  ) {
    // TODO: End layout switch immediately to not obstruct screen for debugging purposes,
    //  and anyway that new loading screen sucks.
    LobbyLayoutNotifyModelBeginLayoutSwitchEvent(LayoutState.BATTLE).schedule(lobby)
    LobbyLayoutNotifyModelEndLayoutSwitchEvent(LayoutState.BATTLE, LayoutState.BATTLE).schedule(lobby)
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)

    DispatcherUnloadObjectsManagedEvent(
      listOf(
        chat.gameObject,
        battleSelect.gameObject
      ) + battles.gameObjects
    ).schedule(dispatcher)

    val battleSpace = spaces.get(battleInfo.gameObject.id) ?: throw IllegalStateException("Battle space ${battleInfo.gameObject.id} not found")

    val battleUserObject = TransientGameObject.instantiate(
      id = TransientGameObject.transientId("BattleUser:${user.gameObject.id}:${Clock.System.now().toEpochMilliseconds()}"),
      parent = TemplatedGameClass.fromTemplate(BattleUserTemplate::class),
      BattleUserTemplate(
        battleUser = BattleUserComponent(),
        userGroup = UserGroupComponent(user.gameObject),
        team = TeamComponent(event.team),
        spectator = null,
      )
    )
    battleSpace.objects.add(battleUserObject)

    DispatcherOpenSpaceEvent(battleInfo.gameObject.id).schedule(dispatcher).await()
    if(BattlefieldReplayMiddleware.replayWriter != null) {
      BattlefieldReplayMiddleware.replayWriter!!.writeComment("user id: ${user.gameObject.id}")
      BattlefieldReplayMiddleware.replayWriter!!.writeComment("battle user id: ${battleUserObject.id}")
    } else {
      startReplay(battleSpace)
    }
  }

  // TODO: Repeats logic from [fight]
  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun joinAsSpectator(
    event: BattleEntranceModelJoinAsSpectatorEvent,
    battleInfo: BattleInfoNode,
    @PerChannel battleInfoShared: List<BattleInfoNode>,
    user: UserNode,
    @JoinAll lobby: LobbyNode,
    @JoinAll chat: ChatNode,
    @JoinAll battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll battles: List<BattleInfoNode>,
    @JoinAll dispatcher: DispatcherNode,
  ) {
    // TODO: End layout switch immediately to not obstruct screen for debugging purposes,
    //  and anyway that new loading screen sucks.
    LobbyLayoutNotifyModelBeginLayoutSwitchEvent(LayoutState.BATTLE).schedule(lobby)
    LobbyLayoutNotifyModelEndLayoutSwitchEvent(LayoutState.BATTLE, LayoutState.BATTLE).schedule(lobby)
    LobbyLayoutNotifyModelCancelPredictedLayoutSwitchEvent().schedule(lobby)

    DispatcherUnloadObjectsManagedEvent(
      listOf(
        chat.gameObject,
        battleSelect.gameObject
      ) + battles.gameObjects
    ).schedule(dispatcher)

    val battleSpace = spaces.get(battleInfo.gameObject.id) ?: throw IllegalStateException("Battle space ${battleInfo.gameObject.id} not found")

    val battleUserObject = TransientGameObject.instantiate(
      id = TransientGameObject.transientId("BattleUser:${user.gameObject.id}:${Clock.System.now().toEpochMilliseconds()}"),
      parent = TemplatedGameClass.fromTemplate(BattleUserTemplate::class),
      BattleUserTemplate(
        battleUser = BattleUserComponent(),
        userGroup = UserGroupComponent(user.gameObject),
        team = null,
        spectator = SpectatorComponent(),
      )
    )
    battleSpace.objects.add(battleUserObject)

    DispatcherOpenSpaceEvent(battleInfo.gameObject.id).schedule(dispatcher).await()
    startReplay(battleSpace)
  }

  private fun startReplay(battleSpace: ISpace) {
    GlobalScope.launch {
      delay(3000)
      val userObject = TransientGameObject.instantiate(
        185858060,
        parent = TemplatedGameClass.fromTemplate(UserTemplate::class),
        template = UserTemplate.Provider.create(),
        components = setOf(
          UsernameComponent("REPLAY_User1"),
          ScoreComponent(Random.nextInt(10_000, 1_000_000).roundToNearest(100)),
          CrystalsComponent(Random.nextInt(100_000, 10_000_000).roundToNearest(100)),
        )
      )
      userObject.addComponent(UserGroupComponent(userObject))

      val battleUserObject = TransientGameObject.instantiate(
        id = -218052496,
        parent = TemplatedGameClass.fromTemplate(BattleUserTemplate::class),
        BattleUserTemplate(
          battleUser = BattleUserComponent(),
          userGroup = UserGroupComponent(userObject),
          team = TeamComponent(BattleTeam.NONE),
          spectator = null,
        )
      )
      battleSpace.objects.add(battleUserObject)

      val reader = ReplayReader(battleSpace, userObject)
      reader.readEvents().collect { event ->
        eventScheduler.schedule(event.event, SpaceChannelModelContext(event.sender), event.gameObject)
      }
    }
  }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun exitFromBattleToLobby(
    event: LobbyLayoutModelExitFromBattleToBattleLobbyEvent,
    lobby: LobbyNode,
    @JoinAll @AllowUnloaded chat: ChatNode,
    @JoinAll @AllowUnloaded battleSelect: SingleNode<BattleSelectModelCC>,
    @JoinAll @AllowUnloaded battles: List<BattleInfoNode>,
    @JoinAll dispatcher: DispatcherNode,
  ) {
    // TODO: End layout switch immediately to not obstruct screen for debugging purposes,
    //  and anyway that new loading screen sucks.
    LobbyLayoutNotifyModelBeginLayoutSwitchEvent(LayoutState.BATTLE_SELECT).schedule(lobby)
    LobbyLayoutNotifyModelEndLayoutSwitchEvent(LayoutState.BATTLE_SELECT, LayoutState.BATTLE_SELECT).schedule(lobby)

    // TODO: Workaround, works for now
    val battleChannel = lobby.context.requireSpaceChannel.sessionNotNull.spaces.all.single { it.space.rootObject.models.contains(BattlefieldModelCC::class) }
    battleChannel.close()

    val user = battleChannel.sessionNotNull.userNotNull.adapt<UserNode>(battleChannel)
    val battleUser = battleChannel.space.objects.all.findBy<BattleUserNode, UserGroupComponent>(user)
    UnloadBattleUserEvent().schedule(battleUser)

    // Mirrors loading logic
    DispatcherLoadObjectsManagedEvent(
      chat.gameObject,
      battleSelect.gameObject
    ).schedule(dispatcher).await()

    // TODO: NodeAddedEvent is not yet automatically scheduled
    NodeAddedEvent().schedule(chat)
    NodeAddedEvent().schedule(battleSelect)
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
