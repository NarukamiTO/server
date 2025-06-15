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

package jp.assasans.narukami.server.battlefield

import kotlin.io.path.readText
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.chat.BattleChatModelAddSystemMessageEvent
import jp.assasans.narukami.server.battlefield.chat.BattleDebugMessageEvent
import jp.assasans.narukami.server.battlefield.replay.ReplayRecord
import jp.assasans.narukami.server.battlefield.replay.ReplaySocketClient
import jp.assasans.narukami.server.battlefield.tank.*
import jp.assasans.narukami.server.battlefield.tank.hull.HullTemplate
import jp.assasans.narukami.server.battlefield.tank.paint.PaintTemplate
import jp.assasans.narukami.server.battlefield.tank.weapon.isida.IsidaTemplate
import jp.assasans.narukami.server.battleselect.*
import jp.assasans.narukami.server.battleservice.StatisticsDMModelUserConnectEvent
import jp.assasans.narukami.server.battleservice.StatisticsDMModelUserDisconnectEvent
import jp.assasans.narukami.server.battleservice.UserInfo
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.GameObjectIdSource
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.dispatcher.DispatcherLoadDependenciesManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherUnloadObjectsManagedEvent
import jp.assasans.narukami.server.garage.item.*
import jp.assasans.narukami.server.lobby.UserNode
import jp.assasans.narukami.server.lobby.UsernameComponent
import jp.assasans.narukami.server.lobby.communication.ChatModeratorLevel
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.protocol.ProtocolEvent
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.ProplibRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

@ProtocolEvent(-1)
@ReplayRecord
class UnloadBattleUserEvent : IEvent

data class TankNode(
  val tank: TankModelCC,
  val hullGroup: HullGroupComponent,
  val weaponGroup: WeaponGroupComponent,
  val paintGroup: PaintGroupComponent,
) : Node()

data class TankLogicStateComponent(var logicState: TankLogicState) : IComponent

class BattleUserComponent : IComponent
class SpectatorComponent : IComponent
data class TeamComponent(val team: BattleTeam) : IComponent

class UserGroupComponent(
  /**
   * Real user ID that identifies a group of objects.
   */
  override val key: Long
) : GroupComponent(key)

object BattleUserTemplate : PersistentTemplateV2() {
  fun create(id: Long, user: IGameObject) = gameObject(id).apply {
    addComponent(BattleUserComponent())
    addComponent(user.getComponent<UserGroupComponent>())
  }

  // For deserializing extern objects
  override fun instantiate(id: Long) = gameObject(id)
}

data class BattleUserNode(
  val battleUser: BattleUserComponent,
  val userGroup: UserGroupComponent,
  val team: TeamComponent?,
  val spectator: SpectatorComponent?,
) : Node()

fun BattleUserNode.asUserInfo(objects: IRegistry<IGameObject>): UserInfo {
  val user = objects.all.findBy<UserNode, UserGroupComponent>(this)
  val tank = objects.all.findBy<TankNode, UserGroupComponent>(this)
  return UserInfo(
    chatModeratorLevel = ChatModeratorLevel.ADMINISTRATOR,
    deaths = 0,
    hasPremium = false,
    kills = 0,
    rank = 1,
    score = 0,
    uid = user.username.username,
    user = tank.gameObject.id,
  )
}

fun BattleUserNode.asBattleInfoUser(): BattleInfoUser {
  return BattleInfoUser(
    clanId = 0,
    score = 0,
    suspicious = false,
    user = gameObject.id,
  )
}

data class BattleMapNode(
  val battleMap: BattleMapModelCC,
  val battleInfoGroup: BattleInfoGroupComponent,
) : Node()

class BattlefieldSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()
  private val objectMapper: ObjectMapper by inject()
  private val spaces: IRegistry<ISpace> by inject()

  @OnEventFire
  @OutOfOrderExecution
  suspend fun channelAdded(
    event: ChannelAddedEvent,
    dispatcher: DispatcherNode,
    // XXX: @AllowUnloaded because object is loaded in different space
    @AllowUnloaded user: UserNode,
    @JoinAll @JoinBy(UserGroupComponent::class) @AllowUnloaded battleUser: BattleUserNode,
    @PerChannel dispatcherShared: List<DispatcherNode>,
    @JoinAll @AllowUnloaded battleMap: BattleMapNode,
    @JoinAll @AllowUnloaded battlefield: SingleNode<BattlefieldModelCC>,
    @JoinAll @PerChannel battlefieldShared: List<SingleNode<BattlefieldModelCC>>,
    @JoinAll @AllowUnloaded battleUsers: List<BattleUserNode>,
  ) {
    if(dispatcher.context.requireSpaceChannel.socket is ReplaySocketClient) {
      logger.info { "Dispatchers shared: $dispatcherShared" }
      check(dispatcherShared.size == 2)
    }

    val text = gameResourceRepository.resolve(battleMap.battleMap.mapResource, "private.json").readText()
    val data = objectMapper.readValue<PrivateMapDataEntity>(text)

    DispatcherLoadDependenciesManagedEvent(
      classes = emptyList(),
      resources = data.proplibs.map {
        gameResourceRepository.get(
          it.name,
          it.namespaces,
          ProplibRes,
          Eager
        )
      }
    ).schedule(dispatcher).await()

    DispatcherLoadObjectsManagedEvent(
      battleMap.gameObject,
      battlefield.gameObject,
    ).schedule(dispatcher).await()

    logger.debug { "Battle user: $battleUser" }
    BattleDebugMessageEvent(
      "Battle user: ${battleUser.userGroup}, team: ${battleUser.team?.team}, spectator: ${battleUser.spectator != null}"
    ).schedule(battlefield)

    if(battleUser.team != null) {
      check(battleUser.spectator == null)

      if(dispatcher.context.requireSpaceChannel.socket !is ReplaySocketClient) {
        // TODO: There is no good API for cross-space communication. I don't like this code, should refactor it.
        //  In this case, we need to send an event from the battle space to all channels in the lobby space, excluding self.
        val lobbyChannel = event.channel.sessionNotNull.spaces.get(Space.stableId("lobby")) ?: throw IllegalStateException("No lobby channel")
        val battleInfoObject = lobbyChannel.space.objects.get(battleMap.battleInfoGroup.key) ?: error("Battle info object not found for battle map ${battleMap.gameObject.id}")
        AddBattleUserEvent(battleUser).schedule(lobbyChannel, battleInfoObject)
      }

      // TODO: There is no good API for cross-space communication. I don't like this code, should refactor it.
      val garageSpace = spaces.get(Space.stableId("garage")) ?: throw IllegalStateException("No garage space")

      val hullMarketItem = garageSpace.objects.all.filter {
        it.template is HullGarageItemTemplate &&
        !it.hasComponent<CompositeModificationGarageItemComponent>() &&
        it.getComponent<NameComponent>().name == "Hornet" &&
        it.getComponent<ModificationComponent>().modification == 3
      }.random()

      // TODO: Hack, should have different templates for each weapon
      val weaponMarketItem = garageSpace.objects.all.filter {
        it.template is WeaponGarageItemTemplate &&
        !it.hasComponent<CompositeModificationGarageItemComponent>() &&
        it.getComponent<NameComponent>().name == "Isida"
      }.random()

      val paintMarketItem = garageSpace.objects.all.filter {
        it.template is PaintGarageItemTemplate &&
        it.getComponent<NameComponent>().name == "Holiday"
      }.random()

      val hullObject = HullTemplate.create(GameObjectIdSource.transientId("Hull:${battleUser.gameObject.id}"), hullMarketItem)
      val weaponObject = IsidaTemplate.create(GameObjectIdSource.transientId("Weapon:${battleUser.gameObject.id}"), weaponMarketItem)
      val paintObject = PaintTemplate.create(GameObjectIdSource.transientId("Paint:${battleUser.gameObject.id}"), paintMarketItem)
      val tankObject = TankTemplate.create(
        user.userGroup.key,
        user.gameObject,
        hullObject,
        weaponObject,
        paintObject
      )
      hullObject.addComponent(TankGroupComponent(tankObject))
      weaponObject.addComponent(TankGroupComponent(tankObject))
      paintObject.addComponent(TankGroupComponent(tankObject))

      event.channel.space.objects.add(hullObject)
      event.channel.space.objects.add(weaponObject)
      event.channel.space.objects.add(paintObject)
      event.channel.space.objects.add(tankObject)

      /* Forward loading: load current player to self and existing */
      // UserConnect must be sent before loading the tank object, otherwise an
      // unhelpful error #1009 in [ActionOutputLine::createUserLabel] occurs.
      // Tank object ID must match the user object ID, this is hardcoded in the client.
      val usersInfoForward = battleUsers.filter { it.team != null }.map { it.asUserInfo(event.channel.space.objects) }
      logger.debug { "Forward: $usersInfoForward" }
      StatisticsDMModelUserConnectEvent(
        tankObject.id,
        usersInfoForward
      ).schedule(battlefieldShared)

      logger.debug { "${event.channel.sessionNotNull.userNotNull.getComponent<UsernameComponent>()} Loading tank parts for $dispatcherShared" }
      for(dispatcherRemote in dispatcherShared) {
        logger.debug { "Loading tank parts to ${dispatcherRemote.context.requireSpaceChannel.sessionNotNull.userNotNull.getComponent<UsernameComponent>()}" }
        DispatcherLoadObjectsManagedEvent(
          hullObject,
          weaponObject,
          paintObject,
          tankObject,
        ).schedule(dispatcherRemote).await()
      }
      logger.debug { "${event.channel.sessionNotNull.userNotNull.getComponent<UsernameComponent>()} Loaded tank parts, ${dispatcherShared.size} shared" }
    } else {
      check(battleUser.spectator != null)

      BattleChatModelAddSystemMessageEvent(
        message = buildString {
          appendLine("== Global controls")
          appendLine("\\ – Switch HUD display levels")
          appendLine("Ctrl + 0-9 – save current position")
          appendLine("0-9 – move to saved position")
          appendLine()
          appendLine("== Freecam controls")
          appendLine("W, A, S, D – movement")
          appendLine("E, Q – up / down")
          appendLine("Shift (hold) – speedup")
          appendLine("Arrow keys or hold and drag mouse – rotation")
          appendLine("Space – altitude axis lock")
          appendLine()

          appendLine("== Follow controls")
          appendLine("TAB → LMB → Spectate – follow a player's tank")
          appendLine("Ctrl+F – follow a player closest to the camera")
          appendLine("Ctrl+B – follow a player (from team Blue) closest to the camera")
          appendLine("Ctrl+R – follow a player (from team Red) closest to the camera")
          appendLine("Arrow left / right – switch between players of the same team")
          appendLine("Ctrl+U – deactivate follow mode")
        },
      ).schedule(battlefield)
    }

    val backwardTanks = event.channel.space.objects.all.findAllBy<TankNode, UserGroupComponent>(battleUsers.filter { it.team != null } - battleUser)

    /* Backward loading: load existing players to current - forward loading rules apply */
    for(tank in backwardTanks) {
      val hullObject = event.channel.space.objects.get(tank.hullGroup.key) ?: error("Hull object not found for tank ${tank.gameObject.id}")
      val weaponObject = event.channel.space.objects.get(tank.weaponGroup.key) ?: error("Weapon object not found for tank ${tank.gameObject.id}")
      val paintObject = event.channel.space.objects.get(tank.paintGroup.key) ?: error("Paint object not found for tank ${tank.gameObject.id}")

      DispatcherLoadObjectsManagedEvent(
        hullObject,
        weaponObject,
        paintObject,
        tank.gameObject,
      ).schedule(dispatcher).await()

      // TODO: State check
      TankSpawnerModelSpawnEvent(
        team = BattleTeam.NONE,
        position = Vector3d(x = 0.0f, y = 0.0f, z = 200.0f),
        orientation = Vector3d(x = 0.0f, y = 0.0f, z = 0.0f),
        health = 1000,
        incarnationId = 0,
      ).schedule(battlefield.context, tank.gameObject)

      // TODO: State check again
      TankModelActivateTankEvent().schedule(battlefield.context, tank.gameObject)
    }
  }

  @OnEventFire
  @Mandatory
  fun readyToSpawn(
    event: TankSpawnerModelReadyToSpawnEvent,
    tank: TankNode,
  ) {
    logger.debug { "Process ready-to-spawn" }

    // Sending this starts the game rendering on the client. The client calls
    // [SpawnCameraConfigurator#setupCamera], which sets up the camera.
    //
    // Once this event is received, the client will send [TankSpawnerModelSetReadyToPlaceEvent] after
    // [BattlefieldModelCC.respawnDuration] milliseconds.
    TankSpawnerModelPrepareToSpawnEvent(
      Vector3d(0f, 0f, 200f),
      Vector3d(0f, 0f, 0f),
    ).schedule(tank)
  }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun readyToPlace(
    event: TankSpawnerModelSetReadyToPlaceEvent,
    tank: TankNode,
    @JoinAll @PerChannel battlefieldShared: List<SingleNode<BattlefieldModelCC>>,
  ) {
    logger.debug { "Process ready-to-place, ${battlefieldShared.size} shared" }

    val logicStateComponent = tank.gameObject.getComponent<TankLogicStateComponent>()

    // Spawn current tank for the entire battlefield
    logicStateComponent.logicState = TankLogicState.ACTIVATING
    for(battlefieldRemote in battlefieldShared) {
      TankSpawnerModelSpawnEvent(
        team = BattleTeam.NONE,
        position = Vector3d(x = 0.0f, y = 0.0f, z = 200.0f),
        orientation = Vector3d(x = 0.0f, y = 0.0f, z = 0.0f),
        health = 1000,
        incarnationId = 0,
      ).schedule(battlefieldRemote.context, tank.gameObject)
    }

    // TODO: Check whether tank can be activated, see [handleCollisionWithOtherTank]
    // Spawn -> active delay
    delay(2000)

    // Activate current tank for the entire battlefield
    logicStateComponent.logicState = TankLogicState.ACTIVE
    for(battlefieldRemote in battlefieldShared) {
      TankModelActivateTankEvent().schedule(battlefieldRemote.context, tank.gameObject)
    }
  }

  @OnEventFire
  @Mandatory
  fun confirmSpawn(event: TankSpawnerModelConfirmSpawnEvent, tank: TankNode) {
    // TODO: Unimplemented, not sure what it is used for
  }

  @OnEventFire
  @Mandatory
  suspend fun unloadBattleUser(
    event: UnloadBattleUserEvent,
    // @AllowUnloaded because it is server-only object
    @AllowUnloaded battleUser: BattleUserNode,
    // XXX: @AllowUnloaded because object is loaded in different space
    @AllowUnloaded user: UserNode,
    @JoinAll battlefield: SingleNode<BattlefieldModelCC>,
    @JoinAll battleMap: BattleMapNode,
    @JoinAll dispatcher: DispatcherNode,
    @JoinAll @PerChannel dispatcherShared: List<DispatcherNode>,
    @JoinAll @PerChannel battlefieldShared: List<SingleNode<BattlefieldModelCC>>,
  ) {
    logger.info { "Destroying battle user ${battleUser.userGroup}" }

    battleUser.context.requireSpaceChannel.close()

    val space = battleUser.context.space
    space.objects.remove(user.gameObject)
    space.objects.remove(battleUser.gameObject)

    // TODO: Do not use component nullability checks
    if(battleUser.team != null) {
      val tank = space.objects.all.findBy<TankNode, UserGroupComponent>(battleUser)

      StatisticsDMModelUserDisconnectEvent(tank.gameObject.id).schedule(battlefieldShared - battlefield)

      if(dispatcher.context.requireSpaceChannel.socket !is ReplaySocketClient) {
        // TODO: There is no good API for cross-space communication. I don't like this code, should refactor it.
        //  In this case, we need to send an event from the battle space to all channels in the lobby space, excluding self.
        val lobbyChannel = battleUser.context.requireSpaceChannel.sessionNotNull.spaces.get(Space.stableId("lobby")) ?: throw IllegalStateException("No lobby channel")
        val battleInfoObject = lobbyChannel.space.objects.get(battleMap.battleInfoGroup.key) ?: error("Battle info object not found for battle map ${battleMap.gameObject.id}")
        RemoveBattleUserEvent(battleUser).schedule(lobbyChannel, battleInfoObject)
      }

      space.objects.remove(tank.gameObject)

      val hullObject = space.objects.get(tank.hullGroup.key) ?: error("Hull object not found for tank ${tank.gameObject.id}")
      val weaponObject = space.objects.get(tank.weaponGroup.key) ?: error("Weapon object not found for tank ${tank.gameObject.id}")
      val paintObject = space.objects.get(tank.paintGroup.key) ?: error("Paint object not found for tank ${tank.gameObject.id}")

      space.objects.remove(hullObject)
      space.objects.remove(weaponObject)
      space.objects.remove(paintObject)

      DispatcherUnloadObjectsManagedEvent(
        hullObject,
        weaponObject,
        paintObject,
        tank.gameObject,
      ).schedule(dispatcherShared - dispatcher)
    }
  }
}
