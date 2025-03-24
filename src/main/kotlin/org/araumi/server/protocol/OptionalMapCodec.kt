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

import io.netty.buffer.ByteBuf

class OptionalMapCodec {
  companion object {
    private const val INPLACE_MASK_1_BYTES = 0x20 // 00100000
    private const val INPLACE_MASK_2_BYTES = 0x40 // 01000000
    private const val INPLACE_MASK_3_BYTES = 0x60 // 01100000

    private const val MASK_LENGTH_1_BYTES = 0x80  // 10000000
    private const val MASK_LENGTH_2_BYTES = 0x40  // 01000000
  }

  fun decode(input: ByteBuf): OptionalMap {
    val optionalMap: OptionalMap

    val byte1 = input.readByte()
    if(byte1.toInt() and MASK_LENGTH_1_BYTES != 0x0) {
      val masked = byte1.toInt() and 0x3F // Drop [MASK_LENGTH_1_BYTES] and [MASK_LENGTH_2_BYTES] from the length

      val byteLength = if(byte1.toInt() and MASK_LENGTH_2_BYTES != 0) {
        // Length is <= 0x2000000
        (masked shl 16) or
          (input.readByte().toInt() and 0xFF shl 8) or
          (input.readByte().toInt() and 0xFF)
      } else {
        // Length is <= 504
        masked
      }

      val data = ByteArray(byteLength)
      input.readBytes(data)
      optionalMap = OptionalMap(byteLength, ByteArray(byteLength))

      return optionalMap
    }

    val data = ByteArray(4)
    optionalMap = OptionalMap(0, data)

    // Get the in-place optional map length: 2..3 bits of the first byte
    // This value is encoded using [INPLACE_MASK_1_BYTES], [INPLACE_MASK_2_BYTES] and [INPLACE_MASK_3_BYTES]
    when(val length = byte1.toInt() and 0x60 ushr 5) {
      0    -> {
        // 5 LSBs of the first byte
        data[0] = (byte1.toInt() shl 3).toByte()

        optionalMap.init(5, data) // First byte stores 5 optional map bits
      }

      1    -> {
        val byte2 = input.readByte()

        // 5 LSBs of the first byte | 3 MSBs of the second byte
        data[0] = (byte1.toInt() shl 3 or (byte2.toInt() and 0xFF ushr 5)).toByte()
        // 5 LSBs of the second byte
        data[1] = (byte2.toInt() shl 3).toByte()

        optionalMap.init(13, data) // 5 + 3 + 5 bits = 13 bits
      }

      2    -> {
        val byte2 = input.readByte()
        val byte3 = input.readByte()

        // 5 LSBs of the first byte | 3 MSBs of the second byte
        data[0] = (byte1.toInt() shl 3 or (byte2.toInt() and 0xFF ushr 5)).toByte()
        // 5 LSBs of the second byte | 3 MSBs of the third byte
        data[1] = ((byte2.toInt() shl 3) or (byte3.toInt() and 0xFF ushr 5)).toByte()
        // 5 LSBs of the third byte
        data[2] = (byte3.toInt() shl 3).toByte()

        optionalMap.init(21, data) // 5 + 3 + 5 + 3 + 5 bits = 21 bits
      }

      3    -> {
        val byte2 = input.readByte()
        val byte3 = input.readByte()
        val byte4 = input.readByte()

        // 5 LSBs of the first byte | 3 MSBs of the second byte
        data[0] = (byte1.toInt() shl 3 or (byte2.toInt() and 0xFF ushr 5)).toByte()
        // 5 LSBs of the second byte | 3 MSBs of the third byte
        data[1] = ((byte2.toInt() shl 3) or (byte3.toInt() and 0xFF ushr 5)).toByte()
        // 5 LSBs of the third byte | 3 MSBs of the fourth byte
        data[2] = ((byte3.toInt() shl 3) or (byte4.toInt() and 0xFF ushr 5)).toByte()
        // 5 LSBs of the fourth byte
        data[3] = (byte4.toInt() shl 3).toByte()

        optionalMap.init(29, data) // 5 + 3 + 5 + 3 + 5 + 3 + 5 bits = 29 bits
      }

      else -> throw IllegalArgumentException("Invalid OptionalMap length: $length (unreachable)")
    }

    return optionalMap
  }

  fun encode(output: ByteBuf, optionalMap: OptionalMap) {
    val size = optionalMap.size

    when {
      size <= 5         -> {
        output.writeByte(optionalMap.getByte(0) and 0xFF ushr 3)
      }

      size <= 13        -> {
        output.writeByte((optionalMap.getByte(0) and 0xFF ushr 3) or INPLACE_MASK_1_BYTES)
        output.writeByte((optionalMap.getByte(1) and 0xFF ushr 3) or (optionalMap.getByte(0) shl 5))
      }

      size <= 21        -> {
        output.writeByte((optionalMap.getByte(0) and 0xFF ushr 3) or INPLACE_MASK_2_BYTES)
        output.writeByte((optionalMap.getByte(1) and 0xFF ushr 3) or (optionalMap.getByte(0) shl 5))
        output.writeByte((optionalMap.getByte(2) and 0xFF ushr 3) or (optionalMap.getByte(1) shl 5))
      }

      size <= 29        -> {
        output.writeByte((optionalMap.getByte(0) and 0xFF ushr 3) or INPLACE_MASK_3_BYTES)
        output.writeByte((optionalMap.getByte(1) and 0xFF ushr 3) or (optionalMap.getByte(0) shl 5))
        output.writeByte((optionalMap.getByte(2) and 0xFF ushr 3) or (optionalMap.getByte(1) shl 5))
        output.writeByte((optionalMap.getByte(3) and 0xFF ushr 3) or (optionalMap.getByte(2) shl 5))
      }

      size <= 504       -> {
        // Size is stored in bytes, rounded up
        val byteLength = OptionalMap.convertSize(size)
        assert(byteLength < MASK_LENGTH_1_BYTES) { "Length in bytes ($byteLength) must be < MASK_LENGTH_1_BYTE (unreachable)" }

        output.writeByte(byteLength or MASK_LENGTH_1_BYTES)
        output.writeBytes(optionalMap.data, 0, byteLength)
      }

      size <= 0x2000000 -> {
        // Size is stored in bytes, rounded up
        val byteLength = OptionalMap.convertSize(size)

        output.writeByte((byteLength and 0xFF0000 ushr 16) or MASK_LENGTH_1_BYTES or MASK_LENGTH_2_BYTES)
        output.writeByte(byteLength and 0xFF00 ushr 8)
        output.writeByte(byteLength and 0xFF)
        output.writeBytes(optionalMap.data, 0, byteLength)
      }

      else              -> throw IllegalArgumentException("Optional map length overflow: $size >= 0x2000000")
    }
  }
}
