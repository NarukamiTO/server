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

package org.araumi.server.protocol.container

import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.protocol.Codec
import org.araumi.server.protocol.ICodec
import org.araumi.server.protocol.ProtocolBuffer

class ListCodec<T>(private val elementCodec: ICodec<T>) : Codec<List<T>>() {
  private val logger = KotlinLogging.logger { }

  override fun encode(buffer: ProtocolBuffer, value: List<T>) {
    protocol.varIntCodec.encode(buffer, value.size)
    for(element in value) {
      elementCodec.encode(buffer, element)
    }
  }

  override fun decode(buffer: ProtocolBuffer): List<T> {
    val size = protocol.varIntCodec.decode(buffer)
    logger.trace { "Decoding list of size $size" }

    val list = mutableListOf<T>()
    for(index in 0 until size) {
      list.add(elementCodec.decode(buffer))
    }

    return list
  }
}
