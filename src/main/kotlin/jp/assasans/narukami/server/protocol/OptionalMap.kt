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

/**
 * A bit array for sequential reading and writing of boolean values.
 *
 * Bits are stored in a big-endian order, from left to right in each byte.
 */
class OptionalMap(initialSize: Int = 0, initialData: ByteArray = ByteArray(DEFAULT_SIZE)) {
  companion object {
    const val DEFAULT_SIZE = 1000

    fun convertSize(bits: Int): Int {
      // One of the 3 LSBs is set if the size is not byte aligned
      val roundUp = if(bits and 7 != 0) 1 else 0
      return (bits shr 3) + roundUp
    }
  }

  /**
   * The underlying byte array storing the bits.
   */
  var data: ByteArray
    private set

  /**
   * The number of bits stored in the map.
   */
  var size: Int
    private set

  /**
   * The current reading position in the bit array.
   */
  var readPosition: Int = 0
    private set

  init {
    this.size = initialSize
    this.data = initialData
  }

  /**
   * Returns the byte and bit index of a given bit in the optional map.
   */
  private fun getBitAddress(bit: Int): Pair<Int, Int> {
    // Each byte stores 8 values
    val byteIndex = bit shr 3

    // Bits are stored from left to right (i.e. big endian), so we invert the bit index
    val bitIndex = bit and 0b111 xor 0b111

    return Pair(byteIndex, bitIndex)
  }

  private fun get(bit: Int): Boolean {
    val (byteIndex, bitIndex) = getBitAddress(bit)

    val byte = data[byteIndex]
    val mask = 1 shl bitIndex

    return byte.toInt() and mask != 0
  }

  private fun set(bit: Int, set: Boolean) {
    val (byteIndex, bitIndex) = getBitAddress(bit)

    val mask = 1 shl bitIndex
    val value = data[byteIndex].toInt()

    data[byteIndex] = if(set) {
      value or mask
    } else {
      // Invert the mask to clear the bit
      value and (mask xor 0xFF)
    }.toByte()
  }

  /**
   * Adds a bit to the optional map.
   */
  fun add(set: Boolean) {
    set(size++, set)
  }

  /**
   * Clears the optional map.
   */
  fun clear() {
    size = 0
    readPosition = 0
  }

  fun clone(): OptionalMap {
    val map = OptionalMap(size, data.copyOf())
    map.readPosition = readPosition

    return map
  }

  fun flip() {
    if(size == 0) {
      data[0] = 0
    }
    readPosition = 0
  }

  fun get(): Boolean {
    if(readPosition >= size) throw IndexOutOfBoundsException("Read index out of bounds: $readPosition")
    return get(readPosition++)
  }

  fun getByte(byte: Int): Int {
    return data[byte].toInt()
  }

  fun hasNextBit(): Boolean {
    return readPosition < size
  }

  fun init(size: Int, map: ByteArray) {
    this.data = map
    this.size = size
    readPosition = 0
  }

  fun reset() {
    readPosition = 0
  }

  override fun toString() = buildString {
    append("OptionalMap { size: $size, readPosition: $readPosition, map=")
    for(index in readPosition until size) {
      append(if(this@OptionalMap.get(index)) "1" else "0")
    }
    append(" }")
  }
}
