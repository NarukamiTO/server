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

package jp.assasans.narukami.server.garage.item

import kotlin.reflect.KClass
import jp.assasans.narukami.server.core.IComponent

data class GaragePropertiesContainerComponent(
  val components: Map<KClass<out IPropertyComponent>, IPropertyComponent>,
) : IComponent {
  fun hasComponent(type: KClass<out IComponent>): Boolean {
    return components.containsKey(type)
  }

  fun <T : IPropertyComponent> getComponent(type: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return components[type] as T? ?: throw NoSuchElementException("Component $type not found in $this")
  }

  fun <T : IPropertyComponent> getComponentOrNull(type: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return components[type] as T?
  }
}

inline fun <reified T : IPropertyComponent> GaragePropertiesContainerComponent.hasComponent(): Boolean {
  return hasComponent(T::class)
}

inline fun <reified T : IPropertyComponent> GaragePropertiesContainerComponent.getComponent(): T {
  return getComponent(T::class)
}

inline fun <reified T : IPropertyComponent> GaragePropertiesContainerComponent.getComponentOrNull(): T? {
  return getComponentOrNull(T::class)
}
