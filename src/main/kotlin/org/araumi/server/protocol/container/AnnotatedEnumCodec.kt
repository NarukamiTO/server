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

import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.staticFunctions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.extensions.kotlinClass
import org.araumi.server.net.command.IProtocolEnum
import org.araumi.server.protocol.Codec
import org.araumi.server.protocol.ICodec
import org.araumi.server.protocol.IProtocol
import org.araumi.server.protocol.ProtocolBuffer

class AnnotatedEnumCodec<T : IProtocolEnum<R>, R : Any>(
  private val type: KType
) : Codec<T>() {
  private val logger = KotlinLogging.logger { }

  private val representation = type.kotlinClass.declaredMemberProperties.single { it.name == "value" }.returnType
  private lateinit var codec: ICodec<R>

  private val entries = type.kotlinClass.staticFunctions.single { it.name == "values" }.call() as Array<T>
  private val variants = entries.associateBy { it.value }

  override fun init(protocol: IProtocol) {
    super.init(protocol)
    codec = protocol.getCodec(representation) as ICodec<R>
  }

  override fun encode(buffer: ProtocolBuffer, value: T) {
    codec.encode(buffer, value.value)
  }

  override fun decode(buffer: ProtocolBuffer): T {
    val value = codec.decode(buffer)
    return requireNotNull(variants[value]) { "Unknown enum $type variant: $value" }
  }
}
