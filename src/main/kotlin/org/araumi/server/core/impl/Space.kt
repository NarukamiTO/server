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

package org.araumi.server.core.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.core.IGameClass
import org.araumi.server.core.IGameObject
import org.araumi.server.core.IRegistry
import org.araumi.server.core.ISpace

class Space(override val id: Long) : ISpace {
  private val logger = KotlinLogging.logger { }

  companion object {
    val ROOT_CLASS = TransientGameClass(
      id = 0,
      models = listOf()
    )
  }

  override val objects: IRegistry<IGameObject<*>> = Registry("Game object") { id }

  override val rootObject: IGameObject<*>
    get() = objects.get(id) ?: error("No root object for space $id")

  init {
    objects.add(TransientGameObject<IGameClass>(id, ROOT_CLASS))
  }
}
