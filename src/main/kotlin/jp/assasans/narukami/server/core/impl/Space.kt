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
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IRegistry
import jp.assasans.narukami.server.core.ISpace
import jp.assasans.narukami.server.core.StaticModelProvider
import jp.assasans.narukami.server.dispatcher.DispatcherModelCC

class Space(override val id: Long) : ISpace {
  private val logger = KotlinLogging.logger { }

  companion object {
    val ROOT_CLASS = TransientGameClass(
      id = 0,
      models = listOf(DispatcherModelCC::class)
    )
  }

  override val objects: IRegistry<IGameObject> = Registry("Game object") { id }

  override val rootObject: IGameObject
    get() = objects.get(id) ?: error("No root object for space $id")

  init {
    val rootObject = TransientGameObject(id, ROOT_CLASS)
    rootObject.models[DispatcherModelCC::class] = StaticModelProvider(DispatcherModelCC())
    objects.add(rootObject)
  }
}
