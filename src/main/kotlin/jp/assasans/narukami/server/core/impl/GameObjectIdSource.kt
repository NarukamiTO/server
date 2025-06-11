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

import jp.assasans.narukami.server.core.makePersistentStableId
import jp.assasans.narukami.server.core.makeStableTransientId

/**
 * Provides ID generation for game objects.
 */
object GameObjectIdSource {
  /**
   * Generates a stable transient ID for a game object based on a given identifier.
   * The same identifier will always produce the same transient ID.
   *
   * Transient IDs are always negative; persistent IDs are always positive.
   */
  fun transientId(identifier: String): Long = makeStableTransientId("GameObject:$identifier")

  /**
   * Generates a stable persistent ID for a game object based on a given identifier.
   * The same identifier will always produce the same persistent ID.
   *
   * Persistent IDs are always positive; transient IDs are always negative.
   */
  fun persistentId(identifier: String): Long = makePersistentStableId("GameObject:$identifier")
}
