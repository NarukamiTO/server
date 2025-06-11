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

package jp.assasans.narukami.server.battlefield.tank.weapon.isida

import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.battlefield.tank.weapon.TargetHit
import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.protocol.ProtocolEvent
import jp.assasans.narukami.server.protocol.ProtocolModel

@ProtocolModel(7068168268219022867)
data class IsisModelCC(
  val capacity: Float,
  val chargeRate: Float,
  val checkPeriodMsec: Int,
  val coneAngle: Float,
  val dischargeDamageRate: Float,
  val dischargeHealingRate: Float,
  val dischargeIdleRate: Float,
  val radius: Float,
) : IModelConstructor

@ProtocolEvent(1663635222489302521)
data class IsisModelAddEnergyEvent(
  val energyDelta: Int,
) : IClientEvent

@ProtocolEvent(8679688798445210739)
data class IsisModelReconfigureWeaponEvent(
  val dischargeDamageSpeed: Float,
  val dischargeHealingSpeed: Float,
  val dischargeIdleSpeed: Float,
  val radius: Float,
) : IClientEvent

@ProtocolEvent(6127259399604255678)
class IsisModelResetTargetEvent : IClientEvent

@ProtocolEvent(1663619828320147439)
data class IsisModelSetTargetEvent(
  val state: IsisState,
  val hit: TargetHit,
) : IClientEvent

@ProtocolEvent(3768030223558212864)
class IsisModelStopWeaponEvent : IClientEvent

@ProtocolEvent(1354967048436810550)
data class IsisModelResetTargetCommandEvent(
  val time: Int,
) : IServerEvent

@ProtocolEvent(3111059653472267657)
data class IsisModelSetTargetCommandEvent(
  val time: Int,
  val target: IGameObject,
  val targetIncarnation: Short,
  val localHitPoint: Vector3d,
) : IServerEvent

@ProtocolEvent(9158089826830690211)
data class IsisModelStopWeaponCommandEvent(
  val time: Int,
) : IServerEvent

@ProtocolEvent(1356698539614734948)
data class IsisModelTickCommandEvent(
  val time: Int,
  val targetIncarnation: Short,
  val targetPosition: Vector3d,
  val localHitPoint: Vector3d,
) : IServerEvent
