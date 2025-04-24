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

import kotlin.math.min
import kotlin.math.roundToInt
import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.protocol.BitArea
import jp.assasans.narukami.server.protocol.Codec
import jp.assasans.narukami.server.protocol.ProtocolBuffer

private const val ANGLE_FACTOR = (Math.PI / 4096).toFloat()
private const val ANGULAR_VELOCITY_FACTOR = 0.005f
private const val LINEAR_VELOCITY_FACTOR = 1f
private const val POSITION_FACTOR = 1f

private const val CONTROL_MASK = 0b11111
private const val CONTROL_MASK_BITSIZE = 5
private const val POSITION_COMPONENT_BITSIZE = 17
private const val ORIENTATION_COMPONENT_BITSIZE = 13
private const val LINEAR_VELOCITY_COMPONENT_BITSIZE = 13
private const val ANGULAR_VELOCITY_COMPONENT_BITSIZE = 13

private const val BIT_AREA_SIZE = 21

class MoveCommandCodec : Codec<MoveCommand>() {
  override fun encode(buffer: ProtocolBuffer, value: MoveCommand) {
    val bitArea = BitArea(ByteArray(BIT_AREA_SIZE))

    buffer.data.writeByte((value.control.toInt() and CONTROL_MASK) or ((value.turnSpeed.toInt() and 7) shl 5))

    writeVector3(bitArea, value.position, POSITION_COMPONENT_BITSIZE, POSITION_FACTOR)
    writeVector3(bitArea, value.orientation, ORIENTATION_COMPONENT_BITSIZE, ANGLE_FACTOR)
    writeVector3(bitArea, value.linearVelocity, LINEAR_VELOCITY_COMPONENT_BITSIZE, LINEAR_VELOCITY_FACTOR)
    writeVector3(bitArea, value.angularVelocity, ANGULAR_VELOCITY_COMPONENT_BITSIZE, ANGULAR_VELOCITY_FACTOR)

    buffer.data.writeBytes(bitArea.data)
  }

  override fun decode(buffer: ProtocolBuffer): MoveCommand {
    val header = buffer.data.readByte().toInt()
    val control = header and CONTROL_MASK
    val turnSpeed = (header shr CONTROL_MASK_BITSIZE and 0b111).toByte()

    val bitArea = BitArea(ByteArray(BIT_AREA_SIZE))
    bitArea.reset(buffer.data, BIT_AREA_SIZE)

    return MoveCommand(
      control = control.toByte(),
      turnSpeed = turnSpeed,
      position = readVector3(bitArea, POSITION_COMPONENT_BITSIZE, POSITION_FACTOR),
      orientation = readVector3(bitArea, ORIENTATION_COMPONENT_BITSIZE, ANGLE_FACTOR),
      linearVelocity = readVector3(bitArea, LINEAR_VELOCITY_COMPONENT_BITSIZE, LINEAR_VELOCITY_FACTOR),
      angularVelocity = readVector3(bitArea, ANGULAR_VELOCITY_COMPONENT_BITSIZE, ANGULAR_VELOCITY_FACTOR),
    )
  }

  private fun prepareValue(value: Float, mask: Int, factor: Float): Int {
    val factored = (value / factor).roundToInt()
    val masked = if(factored < -mask) 0 else factored - mask
    return min(mask, masked)
  }

  private fun writeVector3(bitArea: BitArea, vector3: Vector3d, size: Int, factor: Float) {
    val mask = 1 shl (size - 1)

    bitArea.write(size, prepareValue(vector3.x, mask, factor))
    bitArea.write(size, prepareValue(vector3.y, mask, factor))
    bitArea.write(size, prepareValue(vector3.z, mask, factor))
  }

  private fun readVector3(bitArea: BitArea, size: Int, factor: Float): Vector3d {
    val mask = 1 shl (size - 1)

    return Vector3d(
      x = (bitArea.read(size) - mask) * factor,
      y = (bitArea.read(size) - mask) * factor,
      z = (bitArea.read(size) - mask) * factor,
    )
  }
}
