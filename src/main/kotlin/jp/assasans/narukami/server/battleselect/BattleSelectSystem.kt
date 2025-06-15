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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.*
import jp.assasans.narukami.server.battlefield.replay.*
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.GameObjectIdSource
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.dispatcher.DispatcherUnloadObjectsManagedEvent
import jp.assasans.narukami.server.extensions.singleOrNullOrThrow
import jp.assasans.narukami.server.garage.GarageModelCC
import jp.assasans.narukami.server.lobby.*
import jp.assasans.narukami.server.lobby.communication.ChatNode
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

data class MapInfoNode(
  val model: MapInfoModelCC,
) : Node()

data class BattleInfoNode(
  val battleInfo: BattleInfoModelCC,
  val battleParamInfo: BattleParamInfoModelCC,
  val battleEntrance: BattleEntranceModelCC,
) : Node()

data class BattleSpaceComponent(val space: ISpace) : IComponent
class BattleInfoGroupComponent(key: Long) : GroupComponent(key) {
  constructor(gameObject: IGameObject) : this(gameObject.id)
}

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

    val battleInfoObject = DMBattleInfoTemplate.create(battleId, mapInfo.gameObject)
    battleInfoObject.addComponent(BattleInfoGroupComponent(battleInfoObject))
    mapInfo.context.space.objects.add(battleInfoObject)

    spaces.add(Space(battleId).apply {
      battleInfoObject.addComponent(BattleSpaceComponent(this))

      objects.remove(rootObject)

      val battleMapObject = BattleMapTemplate.instantiate(GameObjectIdSource.transientId("Map:${mapInfo.gameObject.id}"))
      // TODO: Should BattleMapTemplate just access components from map info object?
      battleMapObject.addAllComponents(mapInfo.gameObject.allComponents.values)
      battleMapObject.addComponent(BattleInfoGroupComponent(battleInfoObject))
      objects.add(battleMapObject)

      val battlefieldObject = BattlefieldTemplate.create(id, battleMapObject)
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

    val battleUserObject = BattleUserTemplate.create(
      id = GameObjectIdSource.transientId("BattleUser:${user.userGroup.key}:${Clock.System.now().toEpochMilliseconds()}"),
      user = user.gameObject
    )
    battleUserObject.addComponent(TeamComponent(event.team))
    battleSpace.objects.add(user.gameObject)
    battleSpace.objects.add(battleUserObject)

    DispatcherOpenSpaceEvent(battleInfo.gameObject.id).schedule(dispatcher).await()
    if(BattlefieldReplayMiddleware.replayWriter != null) {
      BattlefieldReplayMiddleware.replayWriter!!.writeComment("user id: ${user.userGroup.key}")
      BattlefieldReplayMiddleware.replayWriter!!.writeComment("battle user id: ${battleUserObject.id}")
      BattlefieldReplayMiddleware.replayWriter!!.writeUserObject(lobby.context.requireSpaceChannel.sessionNotNull.hash, user.gameObject)
      BattlefieldReplayMiddleware.replayWriter!!.writeExternObject(battleUserObject)

      // val obj = deserializeExternObject(objectMapper, json, battleSpace.objects)
      // logger.info { "Deserialized extern: $obj" }
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

    val battleUserObject = BattleUserTemplate.create(
      id = GameObjectIdSource.transientId("BattleUser:${user.userGroup.key}:${Clock.System.now().toEpochMilliseconds()}"),
      user = user.gameObject
    )
    battleUserObject.addComponent(SpectatorComponent())
    battleSpace.objects.add(user.gameObject)
    battleSpace.objects.add(battleUserObject)

    DispatcherOpenSpaceEvent(battleInfo.gameObject.id).schedule(dispatcher).await()
    startReplay(battleSpace)
  }

  private fun startReplay(battleSpace: ISpace) {
    GlobalScope.launch {
      delay(3000)
      // val userObject = UserTemplate.instantiate(250774142).apply {
      //   addComponent(UsernameComponent("REPLAY_User1"))
      //   addComponent(ScoreComponent(Random.nextInt(10_000, 1_000_000).roundToNearest(100)))
      //   addComponent(CrystalsComponent(Random.nextInt(100_000, 10_000_000).roundToNearest(100)))
      // }
      // userObject.addComponent(UserGroupComponent(userObject))
      //
      // val battleUserObject = BattleUserTemplate.create(
      //   id = -268415476,
      //   user = userObject
      // )
      // battleUserObject.addComponent(TeamComponent(BattleTeam.NONE))
      // battleSpace.objects.add(battleUserObject)

      val reader = ReplayReader(battleSpace)
      reader.readEvents().collect { entry ->
        when(entry) {
          is ReplayExternObject -> {
            // Remap user ID
            if(entry.gameObject.hasComponent<UserGroupComponent>()) {
              val userGroup = entry.gameObject.removeComponent<UserGroupComponent>()
              entry.gameObject.addComponent(UserGroupComponent(isolateId(userGroup.key)))
            }

            battleSpace.objects.add(entry.gameObject)
          }

          is ReplayUser -> {
            // Isolate user ID
            val userGroup = entry.userObject.removeComponent<UserGroupComponent>()
            entry.userObject.addComponent(UserGroupComponent(isolateId(userGroup.key)))

            val username = entry.userObject.removeComponent<UsernameComponent>()
            val replayUsername = "${username.username} [REPLAY]"
            entry.userObject.addComponent(UsernameComponent(replayUsername))

            battleSpace.objects.add(entry.userObject)
            logger.info { "Added user object ${entry.userObject.getComponent<UserGroupComponent>()}" }
          }

          is ReplayEvent -> {
            eventScheduler.schedule(entry.event, SpaceChannelModelContext(entry.sender), entry.gameObject)
          }
        }
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
    val battleChannel = lobby.context.requireSpaceChannel.sessionNotNull.spaces.all.singleOrNullOrThrow { it.space.rootObject.models.contains(BattlefieldModelCC::class) }
    if(battleChannel != null) {
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
    } else {
      // TODO: Workaround, works for now
      val garageChannel = lobby.context.requireSpaceChannel.sessionNotNull.spaces.all.single { it.space.objects.all.any { it.models.contains(GarageModelCC::class) } }
      garageChannel.close()

      // Garage to lobby transition
      DispatcherLoadObjectsManagedEvent(
        battleSelect.gameObject
      ).schedule(dispatcher).await()

      // TODO: NodeAddedEvent is not yet automatically scheduled
      NodeAddedEvent().schedule(battleSelect)
    }
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
