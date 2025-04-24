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

package jp.assasans.narukami.server.battlefield.tank.weapon

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.net.command.IProtocolEnum
import jp.assasans.narukami.server.net.command.ProtocolEnum
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.net.command.ProtocolStruct

@ProtocolModel(2803166101624878775)
data class RotatingTurretModelCC(
  val turretState: TurretStateCommand,
) : IModelConstructor

@ProtocolStruct
data class TurretStateCommand(
  val controlInput: Float,
  val controlType: TurretControlType,
  val direction: Float,
  val rotationSpeedNumber: Byte,
)

@ProtocolEnum
enum class TurretControlType(override val value: Int) : IProtocolEnum<Int> {
  ROTATION_DIRECTION(0),
  TARGET_ANGLE_LOCAL(1),
  TARGET_ANGLE_WORLD(2),
}
