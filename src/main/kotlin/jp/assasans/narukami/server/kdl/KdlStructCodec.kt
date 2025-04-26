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

package jp.assasans.narukami.server.kdl

import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName
import dev.kdl.KdlNode
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.extensions.kotlinClass

class KdlStructCodec<T : Any>(private val type: KType) : IKdlCodec<T> {
  companion object {
    val Factory = object : IKdlCodecFactory<Any> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<Any> {
        assert(!type.isMarkedNullable)
        require(type.kotlinClass.jvmName.startsWith("jp.assasans.narukami.")) {
          "Attempt to create struct codec for illegal type $type"
        }

        return KdlStructCodec(type)
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  override fun decode(reader: KdlReader, node: KdlNode): T {
    val constructor = requireNotNull(type.kotlinClass.primaryConstructor) { "No primary constructor found for $type" }
    @Suppress("UNCHECKED_CAST")
    constructor as KFunction<T>

    val args = constructor.parameters.associateWith { parameter ->
      val property = node.children.singleOrNull { it.name == parameter.name }
                     ?: node.properties.singleOrNull { it.name == parameter.name }?.asNode()
                     ?: throw IllegalArgumentException("Missing children or property ${parameter.name} in $node")
      val codec = reader.getCodec(parameter.type)
      val value = codec.decode(reader, property)
      logger.debug { "Decoded ${type.kotlinClass.qualifiedName}::${property.name}: $value" }

      value
    }

    return constructor.callBy(args)
  }
}
