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

import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import dev.kdl.KdlNode
import dev.kdl.KdlNumber
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.extensions.kotlinClass

class KdlNumberCodec(private val type: KType) : IKdlCodec<Number> {
  companion object {
    val Factory = object : IKdlCodecFactory<Number> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<Number>? {
        if(!type.kotlinClass.isSubclassOf(Number::class)) return null
        assert(!type.isMarkedNullable)

        return KdlNumberCodec(type)
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  override fun decode(reader: KdlReader, node: KdlNode): Number {
    logger.debug { "Decoding number: $node" }

    val argument = node.arguments.single() as KdlNumber<out Number>
    val value = argument.value()
    return when(type.kotlinClass) {
      Byte::class   -> value.toByte()
      Short::class  -> value.toShort()
      Int::class    -> value.toInt()
      Long::class   -> value.toLong()
      Float::class  -> value.toFloat()
      Double::class -> value.toDouble()
      else          -> throw IllegalArgumentException("Invalid number type: ${type.kotlinClass}")
    }
  }
}
