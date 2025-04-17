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

package jp.assasans.narukami.server.core.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.IRegistry

class Registry<T : Any>(
  private val name: String,
  private val idMapper: T.() -> Long
) : IRegistry<T> {
  private val logger = KotlinLogging.logger { }

  private val items: MutableMap<Long, T> = mutableMapOf()

  override val all: Set<T>
    get() = items.values.toSet()

  override fun add(value: T) {
    val id = idMapper(value)
    if(items.contains(id)) {
      throw IllegalArgumentException("$name with ID $id already exists")
    }

    items[id] = value
  }

  override fun remove(value: T) {
    val id = idMapper(value)
    items.remove(id)
  }

  override fun get(id: Long): T? {
    return items[id]
  }

  override fun has(id: Long): Boolean {
    return items.contains(id)
  }
}
