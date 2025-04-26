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
import kotlin.reflect.full.staticFunctions
import dev.kdl.KdlNode
import jp.assasans.narukami.server.extensions.kotlinClass

class KdlEnumCodec<T : Enum<T>>(private val type: KType) : IKdlCodec<T> {
  companion object {
    val Factory = object : IKdlCodecFactory<Any> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<Any>? {
        if(!type.kotlinClass.isSubclassOf(Enum::class)) return null
        assert(!type.isMarkedNullable)

        @Suppress("UNCHECKED_CAST")
        return KdlEnumCodec(type) as IKdlCodec<Any>
      }
    }
  }

  private val entries = type.kotlinClass.staticFunctions.single { it.name == "values" }.call() as Array<T>
  private val variants = entries.associateBy { it.name }

  override fun decode(reader: KdlReader, node: KdlNode): T {
    val name = node.arguments.single().value() as String
    return variants[name] ?: throw IllegalArgumentException("Unknown enum variant $type::$name")
  }
}
