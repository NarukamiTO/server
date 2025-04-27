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

import jp.assasans.narukami.server.dispatcher.DispatcherModelCC

/**
 * Spaces are used for the actual client-server communication.
 *
 * All spaces have a Dispatcher object with ID same as the space ID and class `0`.
 *
 * @see jp.assasans.narukami.server.core.ArchitectureDocs
 */
interface ISpace {
  val id: Long
  val objects: IRegistry<IGameObject>

  val rootObject: IGameObject
}

/**
 * Replaces the root object of a space with a new one.
 */
fun ISpace.replaceRootObject(gameObject: IGameObject) {
  require(gameObject.id == id) { "Root object ID (${gameObject.id}) must be the same as space ID ($id)" }

  // All root objects must have an implicit dispatcher model,
  // otherwise DispatcherNode handlers will fail.
  gameObject.models[DispatcherModelCC::class] = StaticModelProvider(DispatcherModelCC())

  if(objects.has(id)) {
    objects.remove(rootObject)
  }
  objects.add(gameObject)
}
