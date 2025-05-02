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

package jp.assasans.narukami.server.core

/**
 * A registry of [T], basically a [MutableMap] wrapper.
 */
interface IRegistry<T> {
  val all: Set<T>

  fun add(value: T)
  fun remove(value: T)
  fun get(id: Long): T?
  fun has(id: Long): Boolean
}

/**
 * Takes the value out of the registry, removing it from the registry.
 */
fun <T> IRegistry<T>.take(id: Long): T? {
  val value = get(id)
  if(value != null) remove(value)
  return value
}
