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

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battlefield.tank.*
import jp.assasans.narukami.server.battlefield.tank.weapon.RotatingTurretModelCC
import jp.assasans.narukami.server.battlefield.tank.weapon.RotatingTurretModelUpdateClientEvent
import jp.assasans.narukami.server.battlefield.tank.weapon.RotatingTurretModelUpdateServerEvent
import jp.assasans.narukami.server.core.*

data class RotatingTurretNode(
  val rotatingTurret: RotatingTurretModelCC,
) : Node()

class BattlefieldMovementSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  fun move(
    event: TankModelMoveCommandEvent,
    tank: TankNode,
    @PerChannel tankShared: List<TankNode>,
  ) {
    logger.trace { "Move tank $event" }

    // Broadcast to all tanks, except the sender
    TankModelMoveEvent(moveCommand = event.moveCommand).schedule(tankShared - tank)

    // Jump Pad from Tanki X - requires a patch on the client to work properly
    // if(event.moveCommand.control != 0.toByte()) {
    //   TankModelPushEvent(
    //     hitPoint = Vector3d(x = 0.0f, y = 0.0f, z = 0.0f),
    //     force = Vector3d(
    //       x = 0.0f,
    //       y = 1000.0f * 5000000.0f,
    //       z = 20000.0f * 5000000.0f,
    //     )
    //   ).schedule(tank)
    // }
  }

  @OnEventFire
  @Mandatory
  fun control(
    event: TankModelMovementControlCommandEvent,
    tank: TankNode,
    @PerChannel tankShared: List<TankNode>,
  ) {
    logger.trace { "Control command tank $event" }

    // Broadcast to all tanks, except the sender
    TankModelMovementControlEvent(
      control = event.control,
      turnSpeedNumber = event.turnSpeedNumber,
    ).schedule(tankShared - tank)
  }

  @OnEventFire
  @Mandatory
  fun handleCollisionWithOtherTank(event: TankModelHandleCollisionWithOtherTankEvent, tank: TankNode) {
    // TODO: Prevent tank from spawning for around 500 ms while client sends this event.
    //  Apparently, the client does not send this event until the tank is activated.
  }

  @OnEventFire
  @Mandatory
  fun updateTurret(
    event: RotatingTurretModelUpdateServerEvent,
    turret: RotatingTurretNode,
    @PerChannel turretShared: List<RotatingTurretNode>,
  ) {
    logger.trace { "Update turret $event" }

    // Broadcast to all turrets, except the sender
    RotatingTurretModelUpdateClientEvent(
      turretStateCommand = event.turretStateCommand
    ).schedule(turretShared - turret)
  }
}
