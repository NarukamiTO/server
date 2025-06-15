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

package jp.assasans.narukami.server.battlefield.replay

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IRegistry

class FakeGameObjectRegistry(
  val registry: IRegistry<IGameObject>,
  val selfObject: IGameObject,
) : IRegistry<IGameObject> {
  private val logger = KotlinLogging.logger { }

  override val all: Set<IGameObject>
    get() = registry.all + selfObject

  override fun add(value: IGameObject) {
    registry.add(value)
  }

  override fun remove(value: IGameObject) {
    registry.remove(value)
  }

  override fun get(id: Long): IGameObject? {
    logger.info { "Getting object: $id, self: ${selfObject.id}" }
    if(id == selfObject.id) return selfObject
    return registry.get(id)
  }

  override fun has(id: Long): Boolean {
    if(id == selfObject.id) return true
    return registry.has(id)
  }
}
