/*
 * Araumi TO - a server software reimplementation for a certain browser tank game.
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

package org.araumi.server.protocol

/**
 * Codec for encoding and decoding variable-length integers, mostly length fields.
 */
class VarIntCodec : Codec<Int>() {
  companion object {
    private const val LENGTH_2_BYTES = 0x80
    private const val LENGTH_3_BYTES = 0x40
  }

  /**
   * @param [value] length to encode, must be in range `0 >= length > 0x400000`
   */
  override fun encode(buffer: ProtocolBuffer, value: Int) {
    require(value >= 0) { "Length must be >= 0" }

    when {
      value < 0x80     -> {
        buffer.data.writeByte(value and 0x7F)
      }

      value < 0x4000   -> {
        buffer.data.writeByte(value and 0xFF00 shr 8 or LENGTH_2_BYTES)
        buffer.data.writeByte(value and 0xFF)
      }

      value < 0x400000 -> {
        buffer.data.writeByte(value and 0xFF0000 shr 16 or (LENGTH_2_BYTES or LENGTH_3_BYTES))
        buffer.data.writeByte(value and 0xFF00 shr 8)
        buffer.data.writeByte(value and 0xFF)
      }

      else             -> throw IllegalArgumentException("Length ($value) must be < 0x400000")
    }
  }

  /**
   * @return decoded length in range `0 >= length > 0x400000`
   */
  override fun decode(buffer: ProtocolBuffer): Int {
    val byte1 = buffer.data.readByte()
    // If [LENGTH_2_BYTES] is not set, length < 0x80
    if(byte1.toInt() and LENGTH_2_BYTES == 0) return byte1.toInt()

    val byte2 = buffer.data.readByte()
    // If [LENGTH_3_BYTES] is not set, length < 0x4000
    if(byte1.toInt() and LENGTH_3_BYTES == 0) {
      return (byte1.toInt() and 0x3F shl 8) or
        (byte2.toInt() and 0xFF)
    }

    val byte3 = buffer.data.readByte()
    // Both flags are set, length < 0x400000
    return (byte1.toInt() and 0x3F shl 16) or
      (byte2.toInt() and 0xFF shl 8) or
      (byte3.toInt() and 0xFF)
  }
}
