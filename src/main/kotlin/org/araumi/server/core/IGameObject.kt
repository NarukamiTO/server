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

package org.araumi.server.core

import kotlin.reflect.KClass

/**
 * @see org.araumi.server.core.ArchitectureDocs
 */
interface IGameObject {
  val id: Long
  val parent: IGameClass

  val components: MutableMap<KClass<out IComponent>, IComponent>
  val models: MutableMap<KClass<out IModelConstructor>, IModelProvider<*>>
}

fun IGameObject.addComponent(component: IComponent) {
  check(components[component::class] == null) { "Component ${component::class} already exists in $this" }
  components[component::class] = component
}
