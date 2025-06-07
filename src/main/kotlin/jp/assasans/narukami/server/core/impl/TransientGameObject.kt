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
import kotlin.reflect.KProperty1
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.internal.TemplateMember

class GameClassCache {
  private val cache = mutableMapOf<Set<KClass<out IModelConstructor>>, IGameClass>()

  fun getOrCreate(models: Set<KClass<out IModelConstructor>>): IGameClass {
    return cache.getOrPut(models) {
      TransientGameClass(
        id = TransientGameClass.freeId(),
        models
      )
    }
  }
}

class GameObjectV2(
  override val id: Long,
  override val template: TemplateV2,
) : IGameObject, KoinComponent {
  private val gameClassCache: GameClassCache by inject()

  @Deprecated("Use `template` instead", replaceWith = ReplaceWith("template"))
  override val parent: IGameClass
    get() = gameClassCache.getOrCreate(models.keys)

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

class TransientGameObject(
  override val id: Long,
  override val parent: IGameClass,
) : IGameObject {
  companion object {
    private val logger = KotlinLogging.logger { }

    private val lastId = AtomicLong(-21122019)

    /**
     * Generates a new transient ID for a game object.
     *
     * Transient IDs are negative, as opposed to persistent IDs, which are positive.
     */
    @Deprecated("Use transientId() instead", ReplaceWith("transientId(identifier)"), level = DeprecationLevel.ERROR)
    fun freeId(): Long {
      return lastId.getAndDecrement()
    }

    fun transientId(identifier: String): Long = makeStableTransientId("GameObject:$identifier")

    /**
     * Generates a stable ID for a game object.
     *
     * Stable IDs are positive and always the same for the same identifier.
     */
    fun stableId(identifier: String): Long = makeStableId("GameObject:$identifier")

    @Deprecated("Use TemplateV2.create() instead")
    fun <T : ITemplate> instantiate(
      id: Long,
      parent: TemplatedGameClass<T>,
      template: T,
      components: Set<IComponent> = emptySet(),
    ): IGameObject {
      val gameObject = TransientGameObject(id, parent)

      collectModels(template).forEach { (clazz, value) ->
        val provider = when(value) {
          is IModelProvider<*> -> value
          is IModelConstructor -> StaticModelProvider(value)
          else                 -> throw IllegalArgumentException("$value (for $clazz) is not a valid model or model provider")
        }

        gameObject.models[clazz] = provider
        logger.trace { "Instantiated model $provider from $template" }
      }

      collectComponents(template).forEach { (clazz, value) ->
        gameObject.components[clazz] = value
        logger.trace { "Instantiated component $value from $template" }
      }

      for(component in components) {
        gameObject.addComponent(component)
      }

      return gameObject
    }
  }

  override val template: TemplateV2
    get() = TODO("Not supported")

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
    return "TransientGameObject(id=$id, parent=$parent, models=$models)"
  }
}

private fun collectModels(template: ITemplate): Map<KClass<out IModelConstructor>, Any?> {
  val result = mutableMapOf<KClass<out IModelConstructor>, Any?>()

  val templateClass = template::class
  val members = requireNotNull(jp.assasans.narukami.server.derive.templateToMembers[templateClass]) {
    "$templateClass is not registered"
  }

  for((property, member) in members) {
    @Suppress("UNCHECKED_CAST")
    property as KProperty1<ITemplate, *>

    when(member) {
      is TemplateMember.Model     -> {
        result[member.model] = property.get(template)
      }

      is TemplateMember.Component -> {}

      is TemplateMember.Template  -> {
        val nestedTemplate = property.get(template)
        if(nestedTemplate is ITemplate) {
          result.putAll(collectModels(nestedTemplate))
        }
      }
    }
  }

  return result
}

private fun collectComponents(template: ITemplate): Map<KClass<out IComponent>, IComponent> {
  val result = mutableMapOf<KClass<out IComponent>, IComponent>()

  val templateClass = template::class
  val members = requireNotNull(jp.assasans.narukami.server.derive.templateToMembers[templateClass]) {
    "$templateClass is not registered"
  }

  for((property, member) in members) {
    @Suppress("UNCHECKED_CAST")
    property as KProperty1<ITemplate, *>

    when(member) {
      is TemplateMember.Model     -> {
      }

      is TemplateMember.Component -> {
        val component = property.get(template) as IComponent?
        if(component == null) {
          if(!member.nullable) throw IllegalStateException("Component ${member.component} is not nullable but is null in $template")
        } else {
          result[member.component] = component
        }
      }

      is TemplateMember.Template  -> {
        val nestedTemplate = property.get(template)
        if(nestedTemplate is ITemplate) {
          result.putAll(collectComponents(nestedTemplate))
        }
      }
    }
  }

  return result
}
