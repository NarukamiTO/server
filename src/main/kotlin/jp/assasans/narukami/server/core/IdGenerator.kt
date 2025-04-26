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

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.HighwayHash

fun makeStableId(identifier: String): Long {
  val logger = KotlinLogging.logger { }

  val key = longArrayOf(
    0x21122019_21122019,
    0x42424242_42424242,
    0x21122019_21122019,
    0x42424242_42424242,
  )
  val data = identifier.toByteArray()
  val hash = HighwayHash.hash64(data, 0, data.size, key)
  val id = hash and 0xffffffe
  logger.trace { "Generated stable ID $id for $identifier" }

  return id
}
