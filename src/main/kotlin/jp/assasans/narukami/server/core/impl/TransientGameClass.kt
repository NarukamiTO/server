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

import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import jp.assasans.narukami.server.core.IGameClass
import jp.assasans.narukami.server.core.IModelConstructor

data class TransientGameClass(
  override val id: Long,
  override val models: Set<KClass<out IModelConstructor>>,
) : IGameClass {
  companion object {
    private val lastId = AtomicLong(-21122019)

    /**
     * Generates a new transient ID for a game class.
     */
    fun freeId(): Long {
      return lastId.getAndDecrement()
    }
  }
}
