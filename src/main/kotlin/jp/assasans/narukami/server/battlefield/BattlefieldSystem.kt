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
import jp.assasans.narukami.server.battlefield.chat.BattleDebugMessageEvent
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
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.ProplibRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

class UnloadBattleUserEvent : IEvent

data class TankNode(
  val tank: TankModelCC,
  val hullGroup: HullGroupComponent,
  val weaponGroup: WeaponGroupComponent,
  val paintGroup: PaintGroupComponent,
) : Node()

data class TankLogicStateComponent(var logicState: TankLogicState) : IComponent

interface IGroupComponent : IComponent {
  val reference: IGameObject
}

class BattleUserComponent : IComponent
class SpectatorComponent : IComponent
data class TeamComponent(val team: BattleTeam) : IComponent
data class UserGroupComponent(override val reference: IGameObject) : IGroupComponent {
  override fun toString(): String {
    return "UserGroupComponent(reference=${reference.id}, username=${reference.getComponent<UsernameComponent>().username})"
  }
}

object BattleUserTemplate : TemplateV2() {
  fun create(id: Long, user: IGameObject) = gameObject(id).apply {
    addComponent(BattleUserComponent())
    addComponent(UserGroupComponent(user))
  }
}

data class BattleUserNode(
  val battleUser: BattleUserComponent,
  val userGroup: UserGroupComponent,
  val team: TeamComponent?,
  val spectator: SpectatorComponent?,
) : Node()

fun BattleUserNode.asUserInfo(objects: Iterable<IGameObject>): UserInfo {
  val user = userGroup.reference.adapt<UserNode>(context)
  val tank = objects.findBy<TankNode, UserGroupComponent>(this)
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
      "Battle user: ${battleUser.userGroup.reference.getComponent<UsernameComponent>().username}, team: ${battleUser.team?.team}, spectator: ${battleUser.spectator != null}"
    ).schedule(battlefield)

    if(battleUser.team != null) {
      check(battleUser.spectator == null)

      if(dispatcher.context.requireSpaceChannel.socket !is ReplaySocketClient) {
        // TODO: There is no good API for cross-space communication. I don't like this code, should refactor it.
        //  In this case, we need to send an event from the battle space to all channels in the lobby space, excluding self.
        val lobbyChannel = event.channel.sessionNotNull.spaces.get(Space.stableId("lobby")) ?: throw IllegalStateException("No lobby channel")
        AddBattleUserEvent(battleUser).schedule(lobbyChannel, battleMap.battleInfoGroup.reference)
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

      val hullObject = HullTemplate.create(GameObjectIdSource.transientId("Hull:${user.gameObject.id}"), hullMarketItem)
      val weaponObject = IsidaTemplate.create(GameObjectIdSource.transientId("Weapon:${user.gameObject.id}"), weaponMarketItem)
      val paintObject = PaintTemplate.create(GameObjectIdSource.transientId("Paint:${user.gameObject.id}"), paintMarketItem)
      val tankObject = TankTemplate.create(
        user.gameObject.id,
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
      val usersInfoForward = battleUsers.filter { it.team != null }.map { it.asUserInfo(event.channel.space.objects.all) }
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
    }

    val backwardTanks = event.channel.space.objects.all.findAllBy<TankNode, UserGroupComponent>(battleUsers.filter { it.team != null } - battleUser)

    /* Backward loading: load existing players to current - forward loading rules apply */
    for(tank in backwardTanks) {
      DispatcherLoadObjectsManagedEvent(
        tank.hullGroup.reference,
        tank.weaponGroup.reference,
        tank.paintGroup.reference,
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
  fun unloadBattleUser(
    event: UnloadBattleUserEvent,
    // @AllowUnloaded because it is server-only object
    @AllowUnloaded battleUser: BattleUserNode,
    @JoinAll battlefield: SingleNode<BattlefieldModelCC>,
    @JoinAll battleMap: BattleMapNode,
    @JoinAll dispatcher: DispatcherNode,
    @JoinAll @PerChannel dispatcherShared: List<DispatcherNode>,
    @JoinAll @PerChannel battlefieldShared: List<SingleNode<BattlefieldModelCC>>,
  ) {
    logger.info { "Destroying battle user ${battleUser.userGroup.reference.adapt<UserNode>(battleUser.context).username.username}" }

    val space = battleUser.context.space
    space.objects.remove(battleUser.gameObject)

    // TODO: Do not use component nullability checks
    if(battleUser.team != null) {
      val tank = space.objects.all.findBy<TankNode, UserGroupComponent>(battleUser)

      StatisticsDMModelUserDisconnectEvent(tank.gameObject.id).schedule(battlefieldShared - battlefield)

      // TODO: There is no good API for cross-space communication. I don't like this code, should refactor it.
      //  In this case, we need to send an event from the battle space to all channels in the lobby space, excluding self.
      val lobbyChannel = battleUser.context.requireSpaceChannel.sessionNotNull.spaces.get(Space.stableId("lobby")) ?: throw IllegalStateException("No lobby channel")
      RemoveBattleUserEvent(battleUser).schedule(lobbyChannel, battleMap.battleInfoGroup.reference)

      space.objects.remove(tank.gameObject)
      space.objects.remove(tank.hullGroup.reference)
      space.objects.remove(tank.weaponGroup.reference)
      space.objects.remove(tank.paintGroup.reference)

      DispatcherUnloadObjectsManagedEvent(
        tank.hullGroup.reference,
        tank.weaponGroup.reference,
        tank.paintGroup.reference,
        tank.gameObject,
      ).schedule(dispatcherShared - dispatcher)
    }
  }
}
