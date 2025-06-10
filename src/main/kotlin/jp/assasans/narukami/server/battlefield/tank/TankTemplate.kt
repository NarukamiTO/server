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

package jp.assasans.narukami.server.battlefield.tank

import jp.assasans.narukami.server.battlefield.BattleGearScoreModelCC
import jp.assasans.narukami.server.battlefield.BossStateModelCC
import jp.assasans.narukami.server.battlefield.TankLogicStateComponent
import jp.assasans.narukami.server.battlefield.UserGroupComponent
import jp.assasans.narukami.server.battlefield.tank.hull.MarketItemGroupComponent
import jp.assasans.narukami.server.battlefield.tank.pause.TankPauseModelCC
import jp.assasans.narukami.server.battlefield.tank.suicide.SuicideModelCC
import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.extensions.toRadians
import jp.assasans.narukami.server.garage.item.*
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull

object TankTemplate : TemplateV2() {
  fun create(id: Long, user: IGameObject, hull: IGameObject, weapon: IGameObject, paint: IGameObject) = gameObject(id).apply {
    addComponent(TankGroupComponent(this))
    addComponent(UserGroupComponent(user))
    addComponent(TankLogicStateComponent(TankLogicState.NEW))

    val hullProperties = hull.getComponent<MarketItemGroupComponent>().reference.getComponent<GaragePropertiesContainerComponent>()
    val weaponProperties = weapon.getComponent<MarketItemGroupComponent>().reference.getComponent<GaragePropertiesContainerComponent>()

    addModel(TankSpawnerModelCC(incarnationId = 0))
    addModel(
      TankConfigurationModelCC(
        coloringId = paint.id,
        droneId = 0,
        hullId = hull.id,
        weaponId = weapon.id,
      )
    )
    addModel(ClosureModelProvider {
      val local = requireSpaceChannel.sessionNotNull.userNotNull.id == user.id
      TankModelCC(
        health = if(local) -1 else 1000,
        local = local,
        logicState = it.getComponent<TankLogicStateComponent>().logicState,
        movementDistanceBorderUntilTankCorrection = 2000,
        movementTimeoutUntilTankCorrection = 4000,
        tankState = null,
        team = BattleTeam.NONE,
      )
    })
    addModel(TankResistancesModelCC(resistances = listOf()))
    addModel(TankPauseModelCC())
    addModel(
      SpeedCharacteristicsModelCC(
        baseSpeed = hullProperties.getComponent<SpeedComponent>().speed,
        currentSpeed = hullProperties.getComponent<SpeedComponent>().speed,
        baseAcceleration = hullProperties.getComponent<AccelerationComponent>().forwardAcceleration,
        currentAcceleration = hullProperties.getComponent<AccelerationComponent>().forwardAcceleration,
        reverseAcceleration = hullProperties.getComponent<AccelerationComponent>().reverseAcceleration,
        baseTurnSpeed = hullProperties.getComponent<SpeedComponent>().turnSpeed.toRadians(),
        currentTurnSpeed = hullProperties.getComponent<SpeedComponent>().turnSpeed.toRadians(),
        turnAcceleration = hullProperties.getComponent<AccelerationComponent>().forwardTurnAcceleration.toRadians(),
        currentTurretRotationSpeed = weaponProperties.getComponent<TurretRotationSpeedComponent>().turretRotationSpeed.toRadians(),
        baseTurretRotationSpeed = weaponProperties.getComponent<TurretRotationSpeedComponent>().turretRotationSpeed.toRadians(),
        reverseTurnAcceleration = hullProperties.getComponent<AccelerationComponent>().reverseTurnAcceleration.toRadians(),
        sideAcceleration = hullProperties.getComponent<AccelerationComponent>().sideAcceleration,
        turnStabilizationAcceleration = hullProperties.getComponent<AccelerationComponent>().turnStabilizationAcceleration.toRadians(),
      )
    )
    addModel(
      UltimateModelCC(
        chargePercentPerSecond = 0.0f,
        charged = false,
        enabled = false,
      )
    )
    addModel(
      DroneIndicatorModelCC(
        batteryAmount = 0,
        canOverheal = false,
        droneReady = false,
        timeToReloadMs = 0,
      )
    )
    addModel(TankDeviceModelCC(deviceId = null))
    addModel(SuicideModelCC(suicideDelayMS = 1000))
    addModel(TankTemperatureModelCC())
    addModel(ClosureModelProvider {
      val local = requireSpaceChannel.sessionNotNull.userNotNull.id == user.id
      BossStateModelCC(
        enabled = true,
        hullId = hull.id,
        local = local,
        role = BossRelationRole.VICTIM,
        weaponId = weapon.id,
      )
    })

    // Required for remote tanks
    addModel(BattleGearScoreModelCC(score = 2112))
  }
}
