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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import org.araumi.server.net.toHexString

class ProtocolBufferCodec {
  companion object {
    private const val DATA_OFFSET = 4

    private const val BIG_LENGTH_FLAG = 128
    private const val ZIPPED_FLAG = 64
    private const val ZIP_PACKET_SIZE_DELIMITER = 2000
    private const val LONG_SIZE_DELIMITER = 0x4000
  }

  private val logger = KotlinLogging.logger { }

  private val optionalMapCodec = OptionalMapCodec()

  /**
   * Checks if the packet has [BIG_LENGTH_FLAG] flag.
   *
   * @param [value] the first byte of the packet
   */
  private fun isPacketBigLength(value: Byte) = value.toInt() and BIG_LENGTH_FLAG != 0

  /**
   * Checks if the packet is longer than [LONG_SIZE_DELIMITER] bytes and should have [BIG_LENGTH_FLAG] flag set.
   *
   * @param [length] the length of the packet
   */
  private fun isLongSize(length: Int) = length >= LONG_SIZE_DELIMITER

  /**
   * Returns the length of the packet.
   * @param [buffer] packet data, at least 4 bytes
   * @return length of the packet, or null if buffer contains not enough data
   */
  fun getPacketLength(buffer: ByteBuf): Int? {
    if(buffer.readableBytes() < 1) return null
    val byte1 = buffer.readByte()

    if(isPacketBigLength(byte1)) {
      if(buffer.readableBytes() < 3) return null
      val byte2 = buffer.readByte()
      val byte3 = buffer.readByte()
      val byte4 = buffer.readByte()

      // Most significant bit has [BIG_LENGTH_FLAG] set, so we strip it
      return (byte1.toInt() and 0x7F shl 24) or
        (byte2.toInt() and 0xFF shl 16) or
        (byte3.toInt() and 0xFF shl 8) or
        (byte4.toInt() and 0xFF)
    } else {
      if(buffer.readableBytes() < 1) return null
      val byte2 = buffer.readByte()

      // 2 most significant bits are never set for short packets,
      // if the packet length is >= [LONG_SIZE_DELIMITER] then it will have [BIG_LENGTH_FLAG] set.
      return (byte1.toInt() and 0x3F shl 8) or
        (byte2.toInt() and 0xFF)
    }
  }

  /**
   * Writes the packet header to the [buffer].
   * @param [length] length of the packet data
   * @return packet start offset
   */
  private fun writeLength(length: Int, buffer: ByteBuf): Int {
    return if(isLongSize(length)) {
      val position = buffer.writerIndex()

      buffer.writeByte(length ushr 24 or BIG_LENGTH_FLAG)
      buffer.writeByte(length and 0xFF0000 ushr 16)
      buffer.writeByte(length and 0xFF00 ushr 8)
      buffer.writeByte(length and 0xFF)

      position
    } else {
      // We skip first 2 bytes because this is a short packet
      buffer.writerIndex(buffer.writerIndex() + 2)
      val position = buffer.writerIndex()

      buffer.writeByte(length and 0xFF00 shr 8)
      buffer.writeByte(length and 0xFF)

      position
    }
  }

  fun encode(output: ByteBuf, value: ProtocolBuffer) {
    val packetOffset = output.writerIndex()

    output.writerIndex(packetOffset + DATA_OFFSET)
    val dataOffset = output.writerIndex()

    optionalMapCodec.encode(output, value.optionalMap)
    logger.trace { "data length real: ${value.data.readableBytes()}" }
    logger.trace { "data: ${value.data.toHexString()}" }
    output.writeBytes(value.data)
    logger.trace { "hex dump: ${output.toHexString()}" }

    val dataLength = output.writerIndex() - dataOffset
    logger.trace { "data+map length calc: $dataLength" }
    output.markWriterIndex()
    logger.trace { "packet offset: $packetOffset" }
    output.writerIndex(packetOffset)
    output.readerIndex(packetOffset + writeLength(dataLength, output))
    output.resetWriterIndex()
  }

  fun decode(input: ByteBuf, length: Int): ProtocolBuffer {
    val offset = input.readerIndex()
    val optionalMap = optionalMapCodec.decode(input)

    // Packet length does include the length of the optional map,
    // but no one cares about it, we should correct for it
    val dataOffset = input.readerIndex() - offset

    logger.trace { "readBytes: ${length - dataOffset}, packet length: $length, dataOffset: $dataOffset, reader index: ${input.readerIndex()}, readable: ${input.readableBytes()}" }
    val data = input.readBytes(length - dataOffset)

    return ProtocolBuffer(data, optionalMap)
  }
}
