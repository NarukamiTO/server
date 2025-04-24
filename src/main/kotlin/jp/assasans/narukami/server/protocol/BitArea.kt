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

package jp.assasans.narukami.server.protocol

import io.netty.buffer.ByteBuf

private const val BITS_PER_BYTE = 8

class BitArea(var data: ByteArray) {
  private var length: Int
  private var position = 0

  init {
    length = data.size * BITS_PER_BYTE
  }

  /**
   * Converts a byte array to a hexadecimal string representation
   */
  fun arrayToHexString(array: ByteArray): String {
    return array.joinToString(", ") { it.toInt().and(0xFF).toString(16) }
  }

  /**
   * Gets the byte at the specified index
   */
  operator fun get(index: Int): Byte = data[index]

  /**
   * Sets the byte at the specified index
   */
  operator fun set(index: Int, value: Int) {
    data[index] = value.toByte()
  }

  /**
   * Checks if a specific bit is set
   *
   * @param bitPosition The position of the bit to check
   * @return true if the bit is set, false otherwise
   */
  fun getBit(bitPosition: Int): Boolean {
    val byteIndex = bitPosition shr 3
    val bitOffset = (bitPosition and 0x7) xor 0x7
    return (data[byteIndex].toInt() and (1 shl bitOffset)) != 0
  }

  /**
   * Sets or clears a specific bit
   *
   * @param bitPosition The position of the bit to modify
   * @param value Whether to set (true) or clear (false) the bit
   */
  fun setBit(bitPosition: Int, value: Boolean) {
    val byteIndex = bitPosition shr 3
    val bitOffset = (bitPosition and 0x7) xor 0x7

    if(value) {
      data[byteIndex] = (data[byteIndex].toInt() or (1 shl bitOffset)).toByte()
    } else {
      data[byteIndex] = (data[byteIndex].toInt() and ((1 shl bitOffset) xor 0xFF)).toByte()
    }
  }

  /**
   * Reads a specified number of bits from the current position
   *
   * @param numBits Number of bits to read
   * @return The integer value represented by the bits
   * @throws IllegalArgumentException if requested bits exceed 32
   * @throws IllegalStateException if not enough bits are available
   */
  fun read(numBits: Int): Int {
    if(numBits > 32) {
      throw IllegalArgumentException("Cannot read more than 32 bits at once (requested $numBits)")
    }

    if(position + numBits > length) {
      throw IllegalStateException("BitArea is out of data: requested $numBits bits, available ${length - position}")
    }

    var result = 0
    for(i in numBits - 1 downTo 0) {
      if(getBit(position)) {
        result = result or (1 shl i)
      }
      position++
    }
    return result
  }

  /**
   * Writes a value using the specified number of bits at the current position
   *
   * @param numBits Number of bits to write
   * @param value The value to write
   * @throws IllegalArgumentException if requested bits exceed 32
   * @throws IllegalStateException if not enough space is available
   */
  fun write(numBits: Int, value: Int) {
    if(numBits > 32) {
      throw IllegalArgumentException("Cannot write more than 32 bits at once (requested $numBits)")
    }

    if(position + numBits > length) {
      throw IllegalStateException("BitArea overflow: attempt to write $numBits bits, space available: ${length - position}")
    }

    for(i in numBits - 1 downTo 0) {
      setBit(position, (value and (1 shl i)) != 0)
      position++
    }
  }

  fun reset() {
    position = 0
  }

  fun reset(byteBuf: ByteBuf, numBytes: Int) {
    // Make sure we have enough space in our data array
    if(data.size < numBytes) {
      data = ByteArray(numBytes)
    }

    byteBuf.readBytes(data, 0, numBytes)
    position = 0
    length = numBytes * 8
  }
}
