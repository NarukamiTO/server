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

import kotlin.reflect.KClass
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*

class GameObjectV2(
  override val id: Long,
  override val template: TemplateV2,
) : IGameObject, KoinComponent {
  private val gameClassCache: GameClassCache by inject()

  override val parent: IGameClass
    get() = gameClassCache.getOrCreate(template, models.keys)

  val components: MutableMap<KClass<out IComponent>, IComponent> = mutableMapOf()
  override val models: MutableMap<KClass<out IModelConstructor>, IModelProvider<*>> = mutableMapOf()

  override val allComponents: Map<KClass<out IComponent>, IComponent>
    get() = components

  override fun addComponent(component: IComponent) {
    check(components[component::class] == null) { "Component ${component::class} already exists in $this" }
    components[component::class] = component
  }

  override fun hasComponent(type: KClass<out IComponent>): Boolean {
    return components.containsKey(type)
  }

  override fun <T : IComponent> getComponent(type: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return components[type] as T? ?: throw NoSuchElementException("Component $type not found in $this")
  }

  override fun <T : IComponent> getComponentOrNull(type: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return components[type] as T?
  }

  override fun <T : IModelConstructor> addModel(type: KClass<T>, model: IModelProvider<T>) {
    if(models.containsKey(type)) throw IllegalStateException("Model $type already exists in $this")
    models[type] = model
  }

  override fun hasModel(type: KClass<out IModelConstructor>): Boolean {
    return models.containsKey(type)
  }

  override fun <T : IModelConstructor> getModel(type: KClass<T>): IModelProvider<T> {
    @Suppress("UNCHECKED_CAST")
    return models[type] as IModelProvider<T>? ?: throw NoSuchElementException("Model $type not found in $this")
  }

  override fun <T : IModelConstructor> getModelOrNull(type: KClass<T>): IModelProvider<T>? {
    @Suppress("UNCHECKED_CAST")
    return models[type] as IModelProvider<T>?
  }

  override fun toString(): String {
    return "${this::class.simpleName}(id=$id, template=${template::class.simpleName}, ${models.size} models, ${components.size} components)"
  }
}
