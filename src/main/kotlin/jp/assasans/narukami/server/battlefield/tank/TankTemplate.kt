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
import jp.assasans.narukami.server.battlefield.tank.pause.TankPauseModelCC
import jp.assasans.narukami.server.battlefield.tank.suicide.SuicideModelCC
import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull

object TankTemplate : TemplateV2() {
  fun create(id: Long, user: IGameObject, configuration: TankConfigurationModelCC) = gameObject(id).apply {
    addComponent(UserGroupComponent(user))
    addModel(TankSpawnerModelCC(incarnationId = 0))
    addModel(configuration)
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
        hullId = configuration.hullId,
        local = local,
        role = BossRelationRole.VICTIM,
        weaponId = configuration.weaponId,
      )
    })

    // Required for remote tanks
    addModel(BattleGearScoreModelCC(score = 2112))
  }
}
