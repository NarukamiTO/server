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

package jp.assasans.narukami.server.protocol.container

import jp.assasans.narukami.server.protocol.Codec
import jp.assasans.narukami.server.protocol.ICodec
import jp.assasans.narukami.server.protocol.ProtocolBuffer

class MapCodec<K, V>(
  private val keyCodec: ICodec<K>,
  private val valueCodec: ICodec<V>
) : Codec<Map<K, V>>() {
  override fun encode(buffer: ProtocolBuffer, value: Map<K, V>) {
    protocol.varIntCodec.encode(buffer, value.size)
    for((key, value) in value) {
      keyCodec.encode(buffer, key)
      valueCodec.encode(buffer, value)
    }
  }

  override fun decode(buffer: ProtocolBuffer): Map<K, V> {
    val size = protocol.varIntCodec.decode(buffer)
    val map = mutableMapOf<K, V>()
    for(index in 0 until size) {
      val key = keyCodec.decode(buffer)
      val value = valueCodec.decode(buffer)
      map[key] = value
    }

    return map
  }
}
