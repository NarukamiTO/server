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

import kotlin.reflect.KClass

/**
 * @see jp.assasans.narukami.server.core.ArchitectureDocs
 */
interface IGameObject {
  val id: Long

  val parent: IGameClass
  val template: TemplateV2

  val allComponents: Map<KClass<out IComponent>, IComponent>
  val models: MutableMap<KClass<out IModelConstructor>, IModelProvider<*>>

  fun addComponent(component: IComponent)
  fun hasComponent(type: KClass<out IComponent>): Boolean
  fun <T : IComponent> getComponent(type: KClass<T>): T
  fun <T : IComponent> getComponentOrNull(type: KClass<T>): T?

  fun <T : IModelConstructor> addModel(type: KClass<T>, model: IModelProvider<T>)
  fun hasModel(type: KClass<out IModelConstructor>): Boolean
  fun <T : IModelConstructor> getModel(type: KClass<T>): IModelProvider<T>
  fun <T : IModelConstructor> getModelOrNull(type: KClass<T>): IModelProvider<T>?
}

fun IGameObject.addAllComponents(components: Collection<IComponent>) {
  components.forEach { addComponent(it) }
}

inline fun <reified T : IComponent> IGameObject.hasComponent(): Boolean {
  return hasComponent(T::class)
}

inline fun <reified T : IComponent> IGameObject.getComponent(): T {
  return getComponent(T::class)
}

inline fun <reified T : IComponent> IGameObject.getComponentOrNull(): T? {
  return getComponentOrNull(T::class)
}

inline fun <reified T : IComponent> Iterable<IGameObject>.filterHasComponent(): List<IGameObject> {
  return filter { it.hasComponent<T>() }
}

inline fun <reified T : IModelConstructor> IGameObject.addModel(model: IModelProvider<T>) {
  addModel(T::class, model)
}

fun <T : IModelConstructor> IGameObject.addModel(model: T) {
  val type: KClass<T> = model::class as KClass<T>
  val provider: IModelProvider<T> = StaticModelProvider(model)
  addModel(type, provider)
}

inline fun <reified T : IModelConstructor> IGameObject.hasModel(): Boolean {
  return hasModel(T::class)
}

inline fun <reified T : IModelConstructor> IGameObject.getModel(): IModelProvider<T> {
  return getModel(T::class)
}

inline fun <reified T : IModelConstructor> IGameObject.getModelOrNull(): IModelProvider<T>? {
  return getModelOrNull(T::class)
}
