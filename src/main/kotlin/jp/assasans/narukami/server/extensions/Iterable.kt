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

package jp.assasans.narukami.server.extensions

fun <T> Iterable<T>.singleOrNullOrThrow(): T? {
  when(this) {
    is List -> {
      return when(size) {
        0    -> null
        1    -> this[0]
        else -> throw IllegalArgumentException("List contains more than one matching element.")
      }
    }

    else    -> {
      val iterator = iterator()
      if(!iterator.hasNext()) return null

      val single = iterator.next()
      if(iterator.hasNext()) throw IllegalArgumentException("Collection contains more than one matching element.")

      return single
    }
  }
}

inline fun <T> Iterable<T>.singleOrNullOrThrow(predicate: (T) -> Boolean): T? {
  var single: T? = null
  var found = false
  for(element in this) {
    if(predicate(element)) {
      if(found) throw IllegalArgumentException("Collection contains more than one matching element.")
      single = element
      found = true
    }
  }
  if(!found) return null

  @Suppress("UNCHECKED_CAST")
  return single as T
}

inline fun <T> Iterable<T>.singleMap(transform: (T) -> T): T? {
  var single: T? = null
  var found = false
  for(element in this) {
    if(found) throw IllegalArgumentException("Collection contains more than one matching element.")
    single = transform(element)
    found = true
  }
  if(!found) return null

  @Suppress("UNCHECKED_CAST")
  return single as T
}
