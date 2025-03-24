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

import org.araumi.server.protocol.Codec
import org.araumi.server.protocol.ICodec
import org.araumi.server.protocol.ProtocolBuffer

class PairCodec<K, V>(
  private val keyCodec: ICodec<K>,
  private val valueCodec: ICodec<V>
) : Codec<Pair<K, V>>() {
  override fun encode(buffer: ProtocolBuffer, value: Pair<K, V>) {
    keyCodec.encode(buffer, value.first)
    valueCodec.encode(buffer, value.second)
  }

  override fun decode(buffer: ProtocolBuffer): Pair<K, V> {
    val key = keyCodec.decode(buffer)
    val value = valueCodec.decode(buffer)

    return Pair(key, value)
  }
}
