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

package org.araumi.server.protocol.primitive

import kotlin.reflect.KType
import org.araumi.server.protocol.*
import org.araumi.server.res.Resource

class ResourceCodec(
  private val type: KType
) : Codec<Resource<*, *>>() {
  private lateinit var longCodec: ICodec<Long>

  override fun init(protocol: IProtocol) {
    super.init(protocol)
    longCodec = protocol.getTypedCodec<Long>()
  }

  override fun encode(buffer: ProtocolBuffer, value: Resource<*, *>) {
    longCodec.encode(buffer, value.id.id)
  }

  override fun decode(buffer: ProtocolBuffer): Resource<*, *> {
    TODO("Unsupported")
  }
}
