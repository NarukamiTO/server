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
 * Marks an object as part of a group.
 *
 * Objects sharing the same key and group component class are grouped together.
 */
abstract class GroupComponent(
  /**
   * Opaque value that identifies a group of objects.
   */
  open val key: Long
) : IComponent {
  override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(javaClass != other?.javaClass) return false

    other as GroupComponent

    return key == other.key
  }

  override fun hashCode(): Int {
    return key.hashCode()
  }

  override fun toString(): String {
    return "${this::class.simpleName}($key)"
  }
}
