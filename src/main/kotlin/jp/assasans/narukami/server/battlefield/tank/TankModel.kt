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

import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.protocol.ProtocolEvent
import jp.assasans.narukami.server.protocol.ProtocolModel

@ProtocolModel(2150802556932617880)
data class TankModelCC(
  val health: Short,
  val local: Boolean,
  val logicState: TankLogicState,
  val movementDistanceBorderUntilTankCorrection: Int,
  val movementTimeoutUntilTankCorrection: Int,
  val tankState: TankState?,
  val team: BattleTeam,
) : IModelConstructor

@ProtocolEvent(4862541063955121514)
class TankModelActivateTankEvent : IClientEvent

@ProtocolEvent(6442558957060261112)
class TankModelDeathConfirmedEvent : IClientEvent

@ProtocolEvent(3503849245765652149)
data class TankModelKillEvent(
  val killerTankId: Long,
  val respawnDelay: Int,
  val damageType: DamageType,
) : IClientEvent

@ProtocolEvent(3503849245765586498)
data class TankModelMoveEvent(
  val moveCommand: MoveCommand,
) : IClientEvent

@ProtocolEvent(4537830264914226593)
data class TankModelMovementControlEvent(
  val control: Byte,
  val turnSpeedNumber: Byte
) : IClientEvent

@ProtocolEvent(3503849245765491449)
data class TankModelPushEvent(
  val hitPoint: Vector3d,
  val force: Vector3d,
) : IClientEvent

@ProtocolEvent(6244553590046625004)
data class TankModelResetConfigurationEvent(
  val hullId: Long,
  val weaponId: Long,
  val droneId: Long,
  val fullHealthHits: Int,
) : IClientEvent

@ProtocolEvent(2989701651242356625)
data class TankModelSetHealthEvent(
  val newHealth: Float,
) : IClientEvent

@ProtocolEvent(2895186944952018743)
class TankModelDeathConfirmationCommandEvent : IServerEvent

/**
 * Can be used to check when to activate newly spawned tank.
 */
@ProtocolEvent(117278145745427103)
data class TankModelHandleCollisionWithOtherTankEvent(
  val otherTankZVelocity: Float,
) : IServerEvent

@ProtocolEvent(9081012168130771749)
data class TankModelMoveCommandEvent(
  val clientTime: Int,
  val specificationId: Short,
  val moveCommand: MoveCommand,
) : IServerEvent

@ProtocolEvent(4416440204413517838)
data class TankModelMovementControlCommandEvent(
  val clientTime: Int,
  val specificationId: Short,
  val control: Byte,
  val turnSpeedNumber: Byte,
) : IServerEvent
