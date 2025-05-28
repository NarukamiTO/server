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

import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.tank.*
import jp.assasans.narukami.server.battlefield.tank.hull.*
import jp.assasans.narukami.server.battlefield.tank.paint.ColoringModelCC
import jp.assasans.narukami.server.battlefield.tank.paint.ColoringTemplate
import jp.assasans.narukami.server.battlefield.tank.weapon.*
import jp.assasans.narukami.server.battlefield.tank.weapon.smoky.SmokyModelCC
import jp.assasans.narukami.server.battlefield.tank.weapon.smoky.SmokyShootSFXModelCC
import jp.assasans.narukami.server.battlefield.tank.weapon.smoky.SmokyTemplate
import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.battleselect.PrivateMapDataEntity
import jp.assasans.narukami.server.battleservice.StatisticsDMModelUserConnectEvent
import jp.assasans.narukami.server.battleservice.UserInfo
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.NodeBuilder
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.dispatcher.DispatcherLoadDependenciesManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.lobby.UserNode
import jp.assasans.narukami.server.lobby.UsernameComponent
import jp.assasans.narukami.server.lobby.communication.ChatModeratorLevel
import jp.assasans.narukami.server.lobby.user.adaptSingle
import jp.assasans.narukami.server.net.command.ProtocolClass
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.res.*

data class TankNode(
  val tank: TankModelCC,
  val tankConfiguration: TankConfigurationModelCC,
) : Node()

private fun createTankHull(gameResourceRepository: IGameResourceRepository) = TransientGameObject.instantiate(
  id = TransientGameObject.freeId(),
  parent = TemplatedGameClass.fromTemplate(HullTemplate::class),
  HullTemplate(
    hullCommon = HullCommonModelCC(
      deadColoring = gameResourceRepository.get("tank.dead", mapOf(), TextureRes, Eager),
      deathSound = gameResourceRepository.get("tank.sound.destroy", mapOf(), SoundRes, Eager),
      lightingSFXEntity = LightingSFXEntity(
        effects = listOf(
          LightingEffectEntity(
            "explosion", listOf(
              LightEffectItem(1f, 2f, "0xCCA538", 0f, 0),
              LightEffectItem(500f, 1500f, "0xCCA538", 1.2f, 100),
              LightEffectItem(1f, 2f, "0xCCA538", 0f, 1200)
            )
          )
        )
      ),
      mass = 100000f,
      stunEffectTexture = gameResourceRepository.get("tank.stun.texture", mapOf(), TextureRes, Eager),
      stunSound = gameResourceRepository.get("tank.stun.sound", mapOf(), SoundRes, Eager),
      ultimateHudIndicator = gameResourceRepository.get("tank.dead", mapOf(), TextureRes, Eager), // TODO: Wrong
      ultimateIconIndex = 0,
    ),
    simpleArmor = SimpleArmorModelCC(maxHealth = 1000),
    engine = EngineModelCC(
      engineIdleSound = gameResourceRepository.get("tank.sound.idle", mapOf(), SoundRes, Eager),
      engineMovingSound = gameResourceRepository.get("tank.sound.moving", mapOf(), SoundRes, Eager),
      engineStartMovingSound = gameResourceRepository.get("tank.sound.start-move", mapOf(), SoundRes, Eager),
      engineStartSound = gameResourceRepository.get("tank.sound.start-move", mapOf(), SoundRes, Eager),
      engineStopMovingSound = gameResourceRepository.get("tank.sound.idle", mapOf(), SoundRes, Eager),
    ),
    object3DS = gameResourceRepository.get("tank.hull.viking", mapOf("gen" to "1.0", "modification" to "0"), Object3DRes, Eager).asModel(),
    hullSmoke = HullSmokeModelCC(
      alpha = 0.5f,
      density = 1f,
      enabled = true,
      fadeTime = 1000,
      farDistance = 1000f,
      nearDistance = 1f,
      particle = gameResourceRepository.get("tank.smoke", mapOf(), MultiframeTextureRes, Eager),
      size = 1f,
    ),
    tankExplosion = TankExplosionModelCC(
      explosionTexture = gameResourceRepository.get("tank.explosion", mapOf(), MultiframeTextureRes, Eager),
      shockWaveTexture = gameResourceRepository.get("tank.shock-wave", mapOf(), MultiframeTextureRes, Eager),
      smokeTextureId = gameResourceRepository.get("tank.smoke", mapOf(), MultiframeTextureRes, Eager),
    ),
    trackedChassis = TrackedChassisModelCC(damping = 3000f),
  )
)

private fun createTankWeapon(gameResourceRepository: IGameResourceRepository) = TransientGameObject.instantiate(
  id = TransientGameObject.freeId(),
  parent = TemplatedGameClass.fromTemplate(SmokyTemplate::class),
  SmokyTemplate(
    weaponCommon = WeaponCommonModelCC(
      buffShotCooldownMs = 0,
      buffed = false,
      highlightingDistance = 1000f,
      impactForce = 1000f,
      kickback = 1000f,
      turretRotationAcceleration = 1f,
      turretRotationSound = gameResourceRepository.get("tank.sound.weapon-rotate", mapOf(), SoundRes, Eager),
      turretRotationSpeed = 1f
    ),
    object3DS = gameResourceRepository.get("tank.weapon.smoky", mapOf("gen" to "1.0", "modification" to "0"), Object3DRes, Eager).asModel(),
    verticalAutoAiming = VerticalAutoAimingModelCC(),
    rotatingTurret = RotatingTurretModelCC(
      turretState = TurretStateCommand(
        controlInput = 0f,
        controlType = TurretControlType.ROTATION_DIRECTION,
        direction = 0f,
        rotationSpeedNumber = 100
      )
    ),
    discreteShot = DiscreteShotModelCC(reloadMsec = 500),
    weaponWeakening = WeaponWeakeningModelCC(
      maximumDamageRadius = 500.0f,
      minimumDamagePercent = 10.0f,
      minimumDamageRadius = 1000.0f
    ),
    weaponVerticalAngles = WeaponVerticalAnglesModelCC(
      angleDown = 0.2f,
      angleUp = 0.2f
    ),
    splash = SplashModel(
      impactForce = 100f,
      minSplashDamagePercent = 5f,
      radiusOfMaxSplashDamage = 100f,
      splashDamageRadius = 1000f,
    ),
    smoky = SmokyModelCC(),
    smokyShootSFX = SmokyShootSFXModelCC(
      criticalHitSize = 1000,
      criticalHitTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.critical", mapOf(), MultiframeTextureRes, Eager),
      explosionMarkTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.hit-mark", mapOf(), TextureRes, Eager),
      explosionSize = 375,
      explosionSound = gameResourceRepository.get("tank.weapon.smoky.sfx.sound.hit", mapOf(), SoundRes, Eager),
      explosionTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.NC_explosion", mapOf(), MultiframeTextureRes, Eager),
      lightingSFXEntity = LightingSFXEntity(
        listOf(
          LightingEffectEntity(
            "hit", listOf(
              LightEffectItem(attenuationBegin = 170f, attenuationEnd = 300f, color = "0xffbf00", intensity = 1.7f, time = 0),
              LightEffectItem(attenuationBegin = 100f, attenuationEnd = 300f, color = "0xffbf00", intensity = 0f, time = 400)
            )
          ),
          LightingEffectEntity(
            "shot", listOf(
              LightEffectItem(attenuationBegin = 190f, attenuationEnd = 450f, color = "0xfcdd76", intensity = 1.9f, time = 0),
              LightEffectItem(attenuationBegin = 1f, attenuationEnd = 2f, color = "0xfcdd76", intensity = 0f, time = 300)
            )
          ),
          LightingEffectEntity(
            "shell", listOf(
              LightEffectItem(attenuationBegin = 0f, attenuationEnd = 0f, color = "0xfcdd76", intensity = 0f, time = 0)
            )
          )
        )
      ),
      shotSound = gameResourceRepository.get("tank.weapon.smoky.sfx.sound.shot", mapOf(), SoundRes, Eager),
      shotTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.shot", mapOf(), TextureRes, Eager)
    )
  )
)

private fun createTankPaint(gameResourceRepository: IGameResourceRepository) = TransientGameObject.instantiate(
  id = TransientGameObject.freeId(),
  parent = TemplatedGameClass.fromTemplate(ColoringTemplate::class),
  ColoringTemplate(
    coloring = ColoringModelCC.static(
      gameResourceRepository.get("tank.paint.fracture", mapOf("gen" to "2.1"), TextureRes, Eager),
    )
  )
)

data class TankLogicStateComponent(var logicState: TankLogicState) : IComponent

interface IGroupComponent : IComponent {
  val reference: IGameObject
}

class BattleUserComponent : IComponent
data class BattleUserGroupComponent(override val reference: IGameObject) : IGroupComponent
data class UserGroupComponent(override val reference: IGameObject) : IGroupComponent

@ProtocolClass(6464)
data class BattleUserTemplate(
  val battleUser: BattleUserComponent,
  val userGroup: UserGroupComponent,
) : ITemplate

data class BattleUserNode(
  val battleUser: BattleUserComponent,
  val userGroup: UserGroupComponent,
) : Node()

private fun createTank(user: UserNode, configuration: TankConfigurationModelCC) = TransientGameObject.instantiate(
  id = user.gameObject.id,
  parent = TemplatedGameClass.fromTemplate(TankTemplate::class),
  TankTemplate(
    userGroup = UserGroupComponent(user.gameObject),
    tankSpawner = TankSpawnerModelCC(incarnationId = 0),
    tankConfiguration = configuration,
    tank = ClosureModelProvider {
      val local = requireSpaceChannel.sessionNotNull.userNotNull.id == user.gameObject.id
      TankModelCC(
        health = if(local) -1 else 1000,
        local = local,
        logicState = it.adaptSingle<TankLogicStateComponent>().logicState,
        movementDistanceBorderUntilTankCorrection = 2000,
        movementTimeoutUntilTankCorrection = 4000,
        tankState = null,
        team = BattleTeam.NONE,
      )
    },
    tankResistances = TankResistancesModelCC(resistances = listOf()),
    tankPause = TankPauseModelCC(),
    speedCharacteristics = SpeedCharacteristicsModelCC(
      baseSpeed = 12.00f,
      currentSpeed = 12.00f,
      baseAcceleration = 14.00f,
      currentAcceleration = 14.00f,
      reverseAcceleration = 23.00f,
      baseTurnSpeed = 2.62f,
      currentTurnSpeed = 2.62f,
      turnAcceleration = 2.62f,
      currentTurretRotationSpeed = 2.09f,
      baseTurretRotationSpeed = 2.09f,
      reverseTurnAcceleration = 1.5f,
      sideAcceleration = 13.0f,
      turnStabilizationAcceleration = 1.5f,
    ),
    ultimate = UltimateModelCC(
      chargePercentPerSecond = 0.0f,
      charged = false,
      enabled = false,
    ),
    droneIndicator = DroneIndicatorModelCC(
      batteryAmount = 0,
      canOverheal = false,
      droneReady = false,
      timeToReloadMs = 0,
    ),
    tankDevice = TankDeviceModelCC(deviceId = null),
    suicide = SuicideModelCC(suicideDelayMS = 1000),
    tankTemperature = TankTemperatureModelCC(),
    bossStateModel = ClosureModelProvider {
      val local = requireSpaceChannel.sessionNotNull.userNotNull.id == user.gameObject.id
      BossStateModelCC(
        enabled = true,
        hullId = configuration.hullId,
        local = local,
        role = BossRelationRole.VICTIM,
        weaponId = configuration.weaponId,
      )
    },
    gearScoreModel = BattleGearScoreModelCC(score = 2112),
  ),
  components = setOf(
    TankLogicStateComponent(TankLogicState.NEW),
  )
)

private fun createBattleUser(user: UserNode) = TransientGameObject.instantiate(
  id = TransientGameObject.freeId(),
  parent = TemplatedGameClass.fromTemplate(BattleUserTemplate::class),
  BattleUserTemplate(
    battleUser = BattleUserComponent(),
    userGroup = UserGroupComponent(user.gameObject),
  )
)

fun <O : Node> Node.remap(objects: Iterable<IGameObject>, group: KClass<out IGroupComponent>, output: KClass<out O>): O {
  val nodeBuilder = NodeBuilder()
  val nodeDefinition = nodeBuilder.getNodeDefinition(output.createType())
  val sourceGroup = gameObject.components[group] ?: throw IllegalArgumentException("No group component $group in $this")
  for(gameObject in objects) {
    val targetGroup = gameObject.components[group]
    if(sourceGroup != targetGroup) {
      continue
    }

    val node = nodeBuilder.tryBuildLazy(
      nodeDefinition,
      gameObject.models.mapValues { (_, model) ->
        { model.provide(gameObject, context) }
      },
      gameObject.components
    )
    if(node != null) {
      node.init(context, gameObject)
      return node as O
    }
  }

  throw IllegalArgumentException("Failed to remap $this to ${output.qualifiedName} by $sourceGroup ($group)")
}

inline fun <reified O : Node, reified G : IGroupComponent> Node.remap(objects: Iterable<IGameObject>): O {
  return remap(objects, G::class, O::class)
}

inline fun <reified O : Node, reified G : IGroupComponent> Iterable<Node>.remap(objects: Iterable<IGameObject>): List<O> {
  return map { it.remap<O, G>(objects) }
}

fun BattleUserNode.asUserInfo(objects: Iterable<IGameObject>): UserInfo {
  val user = userGroup.reference.makeNode<UserNode>(context)
  val tank = remap<TankNode, UserGroupComponent>(objects.filter { it.components.contains(TankLogicStateComponent::class) })
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

inline fun <reified T : Node> IGameObject.makeNode(context: IModelContext): T {
  val nodeBuilder = NodeBuilder()
  val nodeDefinition = nodeBuilder.getNodeDefinition(T::class.createType())
  val node = nodeBuilder.tryBuildLazy(
    nodeDefinition,
    models.mapValues { (_, model) -> { model.provide(this, context) } },
    components
  ) ?: throw IllegalArgumentException("Failed to build node for $this")
  node.init(context, this)
  return node as T
}

class BattlefieldSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()
  private val objectMapper: ObjectMapper by inject()

  @OnEventFire
  @OutOfOrderExecution
  suspend fun channelAdded(
    event: ChannelAddedEvent,
    dispatcher: DispatcherNode,
    user: UserNode,
    @JoinAllChannels dispatcherShared: List<DispatcherNode>,
    @JoinAll battleMap: SingleNode<BattleMapModelCC>,
    @JoinAll battlefield: SingleNode<BattlefieldModelCC>,
    @JoinAll @JoinAllChannels battlefieldShared: List<SingleNode<BattlefieldModelCC>>,
    @JoinAll battleUsers: List<BattleUserNode>,
  ) {
    // TODO: Temporary solution
    val root = Paths.get(requireNotNull(System.getenv("RESOURCES_ROOT")) { "\"RESOURCES_ROOT\" environment variable is not set" })
    val text = root.resolve("${battleMap.node.mapResource.id.encode()}/private.json").readText()
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

    val battleUserObject = createBattleUser(user)
    val hullObject = createTankHull(gameResourceRepository)
    val weaponObject = createTankWeapon(gameResourceRepository)
    val paintObject = createTankPaint(gameResourceRepository)
    val tankObject = createTank(
      user,
      TankConfigurationModelCC(
        coloringId = paintObject.id,
        droneId = 0,
        hullId = hullObject.id,
        weaponId = weaponObject.id,
      )
    )

    event.channel.space.objects.add(battleUserObject)
    event.channel.space.objects.add(hullObject)
    event.channel.space.objects.add(weaponObject)
    event.channel.space.objects.add(paintObject)
    event.channel.space.objects.add(tankObject)

    val tanks = battleUsers.remap<TankNode, UserGroupComponent>(event.channel.space.objects.all.filter { it.components.contains(TankLogicStateComponent::class) })

    /* Forward loading: load current player to existing */
    // UserConnect must be sent before loading the tank object, otherwise an
    // unhelpful error #1009 in [ActionOutputLine::createUserLabel] occurs.
    // Tank object ID must match the user object ID, this is hardcoded in the client.
    val battleUser = battleUserObject.makeNode<BattleUserNode>(dispatcher.context)
    val usersInfoForward = battleUsers.map { it.asUserInfo(event.channel.space.objects.all) } + battleUser.asUserInfo(event.channel.space.objects.all)
    logger.info { "Forward: $usersInfoForward" }
    StatisticsDMModelUserConnectEvent(
      tankObject.id,
      usersInfoForward
    ).schedule(battlefieldShared)

    logger.info { "${event.channel.sessionNotNull.userNotNull.components[UsernameComponent::class]} Loading tank parts" }
    for(dispatcherRemote in dispatcherShared) {
      logger.info { "Loading tank parts to ${dispatcherRemote.context.requireSpaceChannel.sessionNotNull.userNotNull.components[UsernameComponent::class]}" }
      DispatcherLoadObjectsManagedEvent(
        hullObject,
        weaponObject,
        paintObject,
        tankObject,
      ).schedule(dispatcherRemote).await()
    }
    logger.info { "${event.channel.sessionNotNull.userNotNull.components[UsernameComponent::class]} Loaded tank parts" }

    /* Backward loading: load existing players to current - notes above apply */
    for(tank in tanks - tankObject) {
      DispatcherLoadObjectsManagedEvent(
        // TODO: Workaround, works for now
        requireNotNull(tank.context.space.objects.get(tank.tankConfiguration.hullId)) { "No hull" },
        requireNotNull(tank.context.space.objects.get(tank.tankConfiguration.weaponId)) { "No weapon" },
        requireNotNull(tank.context.space.objects.get(tank.tankConfiguration.coloringId)) { "No paint" },
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
    logger.info { "Process ready-to-spawn" }

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
    @JoinAll @JoinAllChannels battlefieldShared: List<SingleNode<BattlefieldModelCC>>,
  ) {
    logger.info { "Process ready-to-place, ${battlefieldShared.size} shared" }

    // TODO: Workaround (class cast), works for now
    val logicStateComponent = (tank.gameObject.components[TankLogicStateComponent::class] as TankLogicStateComponent)

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
  fun move(
    event: TankModelMoveCommandEvent,
    tank: TankNode,
    @JoinAllChannels @OnlyLoadedObjects tankShared: List<TankNode>,
  ) {
    logger.info { "Move tank $event" }

    // Broadcast to all tanks, except the sender
    TankModelMoveEvent(moveCommand = event.moveCommand).schedule(tankShared - tank)
  }

  @OnEventFire
  @Mandatory
  fun handleCollisionWithOtherTank(event: TankModelHandleCollisionWithOtherTankEvent, tank: TankNode) {
    // TODO: Prevent tank from spawning for around 500 ms while client sends this event
  }
}
