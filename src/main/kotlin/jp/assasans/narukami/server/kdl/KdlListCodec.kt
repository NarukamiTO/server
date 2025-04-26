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
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.extensions.kotlinClass

class KdlListCodec<T>(private val elementCodec: IKdlCodec<T>) : IKdlCodec<List<T>> {
  companion object {
    val Factory = object : IKdlCodecFactory<List<*>> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<List<*>>? {
        if(!type.kotlinClass.isSubclassOf(List::class)) return null
        assert(!type.isMarkedNullable)

        val elementInfo = requireNotNull(type.arguments.single().type) { "Invalid List<T> generic argument" }
        @Suppress("UNCHECKED_CAST")
        return KdlListCodec(reader.getCodec(elementInfo)) as IKdlCodec<List<*>>
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  override fun decode(reader: KdlReader, node: KdlNode): List<T> {
    return node.children.map { child ->
      if(child.name != "-") throw IllegalArgumentException("Invalid list node: $child")

      val item = elementCodec.decode(reader, child)
      logger.debug { "Decoded list item: $item" }
      item
    }
  }
}
