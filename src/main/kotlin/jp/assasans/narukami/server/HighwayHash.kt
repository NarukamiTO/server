// Copyright 2017 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package jp.assasans.narukami.server

/**
 * HighwayHash algorithm.
 * See [HighwayHash on GitHub](https://github.com/google/highwayhash).
 */
class HighwayHash {
  private val v0 = LongArray(4)
  private val v1 = LongArray(4)
  private val mul0 = LongArray(4)
  private val mul1 = LongArray(4)
  private var done = false

  /**
   * @param key0 first 8 bytes of the key
   * @param key1 next 8 bytes of the key
   * @param key2 next 8 bytes of the key
   * @param key3 last 8 bytes of the key
   */
  constructor(key0: Long, key1: Long, key2: Long, key3: Long) {
    reset(key0, key1, key2, key3)
  }

  /**
   * @param key array of size 4 with the key to initialize the hash with
   */
  constructor(key: LongArray) {
    require(key.size == 4) { String.format("Key length (%s) must be 4", key.size) }
    reset(key[0], key[1], key[2], key[3])
  }

  /**
   * Updates the hash with 32 bytes of data. If you can read 4 long values
   * from your data efficiently, prefer using update() instead for more speed.
   * @param packet data array which has a length of at least pos + 32
   * @param pos position in the array to read the first of 32 bytes from
   */
  fun updatePacket(packet: ByteArray, pos: Int) {
    require(pos >= 0) { String.format("Pos (%s) must be positive", pos) }
    require(pos + 32 <= packet.size) { "packet must have at least 32 bytes after pos" }
    val a0 = read64(packet, pos + 0)
    val a1 = read64(packet, pos + 8)
    val a2 = read64(packet, pos + 16)
    val a3 = read64(packet, pos + 24)
    update(a0, a1, a2, a3)
  }

  /**
   * Updates the hash with 32 bytes of data given as 4 longs. This function is
   * more efficient than updatePacket when you can use it.
   * @param a0 first 8 bytes in little endian 64-bit long
   * @param a1 next 8 bytes in little endian 64-bit long
   * @param a2 next 8 bytes in little endian 64-bit long
   * @param a3 last 8 bytes in little endian 64-bit long
   */
  fun update(a0: Long, a1: Long, a2: Long, a3: Long) {
    check(!done) { "Can compute a hash only once per instance" }
    v1[0] += mul0[0] + a0
    v1[1] += mul0[1] + a1
    v1[2] += mul0[2] + a2
    v1[3] += mul0[3] + a3
    for(i in 0..3) {
      mul0[i] = mul0[i] xor (v1[i] and 0xffffffffL) * (v0[i] ushr 32)
      v0[i] += mul1[i]
      mul1[i] = mul1[i] xor (v0[i] and 0xffffffffL) * (v1[i] ushr 32)
    }
    v0[0] += zipperMerge0(v1[1], v1[0])
    v0[1] += zipperMerge1(v1[1], v1[0])
    v0[2] += zipperMerge0(v1[3], v1[2])
    v0[3] += zipperMerge1(v1[3], v1[2])
    v1[0] += zipperMerge0(v0[1], v0[0])
    v1[1] += zipperMerge1(v0[1], v0[0])
    v1[2] += zipperMerge0(v0[3], v0[2])
    v1[3] += zipperMerge1(v0[3], v0[2])
  }

  /**
   * Updates the hash with the last 1 to 31 bytes of the data. You must use
   * updatePacket first per 32 bytes of the data, if and only if 1 to 31 bytes
   * of the data are not processed after that, updateRemainder must be used for
   * those final bytes.
   * @param bytes data array which has a length of at least pos + size_mod32
   * @param pos position in the array to start reading size_mod32 bytes from
   * @param size_mod32 the amount of bytes to read
   */
  fun updateRemainder(bytes: ByteArray, pos: Int, size_mod32: Int) {
    require(pos >= 0) { String.format("Pos (%s) must be positive", pos) }
    require(!(size_mod32 < 0 || size_mod32 >= 32)) { String.format("size_mod32 (%s) must be between 0 and 31", size_mod32) }
    require(pos + size_mod32 <= bytes.size) { "bytes must have at least size_mod32 bytes after pos" }
    val size_mod4 = size_mod32 and 3
    val remainder = size_mod32 and 3.inv()
    val packet = ByteArray(32)
    for(i in 0..3) {
      v0[i] += (size_mod32.toLong() shl 32) + size_mod32
    }
    rotate32By(size_mod32.toLong(), v1)
    for(i in 0 until remainder) {
      packet[i] = bytes[pos + i]
    }
    if((size_mod32 and 16) != 0) {
      for(i in 0..3) {
        packet[28 + i] = bytes[pos + remainder + i + size_mod4 - 4]
      }
    } else {
      if(size_mod4 != 0) {
        packet[16 + 0] = bytes[pos + remainder + 0]
        packet[16 + 1] = bytes[pos + remainder + (size_mod4 ushr 1)]
        packet[16 + 2] = bytes[pos + remainder + (size_mod4 - 1)]
      }
    }
    updatePacket(packet, 0)
  }

  /**
   * Computes the hash value after all bytes were processed. Invalidates the
   * state.
   *
   * NOTE: The 64-bit HighwayHash algorithm is declared stable and no longer subject to change.
   *
   * @return 64-bit hash
   */
  fun finalize64(): Long {
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    done = true
    return v0[0] + v1[0] + mul0[0] + mul1[0]
  }

  /**
   * Computes the hash value after all bytes were processed. Invalidates the state.
   *
   * @return array of size 2 containing 128-bit hash
   */
  fun finalize128(): LongArray {
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    done = true
    val hash = LongArray(2)
    hash[0] = v0[0] + mul0[0] + v1[2] + mul1[2]
    hash[1] = v0[1] + mul0[1] + v1[3] + mul1[3]
    return hash
  }

  /**
   * Computes the hash value after all bytes were processed. Invalidates the state.
   *
   * @return array of size 4 containing 256-bit hash
   */
  fun finalize256(): LongArray {
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    permuteAndUpdate()
    done = true
    val hash = LongArray(4)
    modularReduction(
      v1[1] + mul1[1], v1[0] + mul1[0],
      v0[1] + mul0[1], v0[0] + mul0[0],
      hash, 0
    )
    modularReduction(
      v1[3] + mul1[3], v1[2] + mul1[2],
      v0[3] + mul0[3], v0[2] + mul0[2],
      hash, 2
    )
    return hash
  }

  private fun reset(key0: Long, key1: Long, key2: Long, key3: Long) {
    mul0[0] = -0x24192a2a01b331d1L
    mul0[1] = -0x5bf6c7ddd660ce30L
    mul0[2] = 0x13198a2e03707344L
    mul0[3] = 0x243f6a8885a308d3L
    mul1[0] = 0x3bd39e10cb0ef593L
    mul1[1] = -0x3f530e964a0e7574L
    mul1[2] = -0x41ab9930cb16f394L
    mul1[3] = 0x452821e638d01377L
    v0[0] = mul0[0] xor key0
    v0[1] = mul0[1] xor key1
    v0[2] = mul0[2] xor key2
    v0[3] = mul0[3] xor key3
    v1[0] = mul1[0] xor ((key0 ushr 32) or (key0 shl 32))
    v1[1] = mul1[1] xor ((key1 ushr 32) or (key1 shl 32))
    v1[2] = mul1[2] xor ((key2 ushr 32) or (key2 shl 32))
    v1[3] = mul1[3] xor ((key3 ushr 32) or (key3 shl 32))
  }

  private fun zipperMerge0(v1: Long, v0: Long): Long {
    return (((v0 and 0xff000000L) or (v1 and 0xff00000000L)) ushr 24) or
      (((v0 and 0xff0000000000L) or (v1 and 0xff000000000000L)) ushr 16) or
      (v0 and 0xff0000L) or ((v0 and 0xff00L) shl 32) or
      ((v1 and -0x100000000000000L) ushr 8) or (v0 shl 56)
  }

  private fun zipperMerge1(v1: Long, v0: Long): Long {
    return (((v1 and 0xff000000L) or (v0 and 0xff00000000L)) ushr 24) or
      (v1 and 0xff0000L) or ((v1 and 0xff0000000000L) ushr 16) or
      ((v1 and 0xff00L) shl 24) or ((v0 and 0xff000000000000L) ushr 8) or
      ((v1 and 0xffL) shl 48) or (v0 and -0x100000000000000L)
  }

  private fun read64(src: ByteArray, pos: Int): Long {
    // Mask with 0xffL so that it is 0..255 as long (byte can only be -128..127)
    return (src[pos + 0].toLong() and 0xffL) or ((src[pos + 1].toLong() and 0xffL) shl 8) or
      ((src[pos + 2].toLong() and 0xffL) shl 16) or ((src[pos + 3].toLong() and 0xffL) shl 24) or
      ((src[pos + 4].toLong() and 0xffL) shl 32) or ((src[pos + 5].toLong() and 0xffL) shl 40) or
      ((src[pos + 6].toLong() and 0xffL) shl 48) or ((src[pos + 7].toLong() and 0xffL) shl 56)
  }

  private fun rotate32By(count: Long, lanes: LongArray) {
    for(i in 0..3) {
      val half0 = (lanes[i] and 0xffffffffL)
      val half1 = (lanes[i] ushr 32) and 0xffffffffL
      lanes[i] = ((half0 shl count.toInt()) and 0xffffffffL) or (half0 ushr (32 - count).toInt())
      lanes[i] = lanes[i] or ((((half1 shl count.toInt()) and 0xffffffffL) or
        (half1 ushr (32 - count).toInt())) shl 32)
    }
  }

  private fun permuteAndUpdate() {
    update(
      (v0[2] ushr 32) or (v0[2] shl 32),
      (v0[3] ushr 32) or (v0[3] shl 32),
      (v0[0] ushr 32) or (v0[0] shl 32),
      (v0[1] ushr 32) or (v0[1] shl 32)
    )
  }

  private fun modularReduction(
    a3_unmasked: Long, a2: Long, a1: Long,
    a0: Long, hash: LongArray, pos: Int
  ) {
    val a3 = a3_unmasked and 0x3FFFFFFFFFFFFFFFL
    hash[pos + 1] = a1 xor ((a3 shl 1) or (a2 ushr 63)) xor ((a3 shl 2) or (a2 ushr 62))
    hash[pos + 0] = a0 xor (a2 shl 1) xor (a2 shl 2)
  }

  private fun processAll(data: ByteArray, offset: Int, length: Int) {
    var i = 0
    while(i + 32 <= length) {
      updatePacket(data, offset + i)
      i += 32
    }
    if((length and 31) != 0) {
      updateRemainder(data, offset + i, length and 31)
    }
  }

  companion object {
    //////////////////////////////////////////////////////////////////////////////
    /**
     * NOTE: The 64-bit HighwayHash algorithm is declared stable and no longer subject to change.
     *
     * @param data array with data bytes
     * @param offset position of first byte of data to read from
     * @param length number of bytes from data to read
     * @param key array of size 4 with the key to initialize the hash with
     * @return 64-bit hash for the given data
     */
    fun hash64(data: ByteArray, offset: Int, length: Int, key: LongArray): Long {
      val h = HighwayHash(key)
      h.processAll(data, offset, length)
      return h.finalize64()
    }

    /**
     * @param data array with data bytes
     * @param offset position of first byte of data to read from
     * @param length number of bytes from data to read
     * @param key array of size 4 with the key to initialize the hash with
     * @return array of size 2 containing 128-bit hash for the given data
     */
    fun hash128(data: ByteArray, offset: Int, length: Int, key: LongArray): LongArray {
      val h = HighwayHash(key)
      h.processAll(data, offset, length)
      return h.finalize128()
    }

    /**
     * @param data array with data bytes
     * @param offset position of first byte of data to read from
     * @param length number of bytes from data to read
     * @param key array of size 4 with the key to initialize the hash with
     * @return array of size 4 containing 256-bit hash for the given data
     */
    fun hash256(data: ByteArray, offset: Int, length: Int, key: LongArray): LongArray {
      val h = HighwayHash(key)
      h.processAll(data, offset, length)
      return h.finalize256()
    }
  }
}
