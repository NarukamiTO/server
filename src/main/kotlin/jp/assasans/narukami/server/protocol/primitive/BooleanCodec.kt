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

package jp.assasans.narukami.server.protocol.primitive

import jp.assasans.narukami.server.protocol.Codec
import jp.assasans.narukami.server.protocol.ProtocolBuffer

class BooleanCodec : Codec<Boolean>() {
  override fun encode(buffer: ProtocolBuffer, value: Boolean) {
    val representation = if(value) 1 else 0
    buffer.data.writeByte(representation)
  }

  override fun decode(buffer: ProtocolBuffer): Boolean {
    return when(val value = buffer.data.readByte()) {
      0.toByte() -> false
      1.toByte() -> true
      else       -> throw IllegalArgumentException("Invalid boolean value: $value")
    }
  }
}
