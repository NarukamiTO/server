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

package org.araumi.server.res

data class ResourceId(
  val id: Long,
  val version: Long
) {
  companion object {
    fun decode(parts: List<String>): ResourceId {
      val id = (parts[0].toULong(8) shl 32) or
        (parts[1].toULong(8) shl 16) or
        (parts[2].toULong(8) shl 8) or
        (parts[3].toULong(8) shl 0)
      return ResourceId(
        id = id.toLong(),
        version = parts[4].toLong(8)
      )
    }

    fun decode(encoded: String): ResourceId = decode(encoded.split("/"))
  }

  fun encode(): String {
    return listOf(
      (id shr 32 and 0xffffffff).toString(8),
      (id shr 16 and 0xffff).toString(8),
      (id shr 8 and 0xff).toString(8),
      (id shr 0 and 0xff).toString(8),
      version.toString(8)
    ).joinToString("/")
  }
}
