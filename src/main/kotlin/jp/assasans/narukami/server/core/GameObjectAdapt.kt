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
import kotlin.reflect.full.createType
import jp.assasans.narukami.server.core.impl.NodeBuilder
import jp.assasans.narukami.server.net.SpaceChannel

/**
 * Creates a [Node] instance from the [IGameObject] for the specified [context].
 *
 * This is useful for performing a type-safe conversion from a game object to a node.
 */
fun <T : Node> IGameObject.adapt(context: IModelContext, clazz: KClass<T>): T {
  val builder = NodeBuilder()
  val definition = builder.getNodeDefinition(clazz.createType())
  val node = builder.tryBuildLazy(definition, this, context)
             ?: throw IllegalStateException("Failed to build node $clazz")
  return node as T
}

/**
 * Creates a [Node] instance from the [IGameObject] for the specified [context].
 *
 * This is useful for performing a type-safe conversion from a game object to a node.
 */
inline fun <reified T : Node> IGameObject.adapt(context: IModelContext): T {
  return adapt(context, T::class)
}

/**
 * Creates a [Node] instance from the [IGameObject] for the specified [sender].
 *
 * This is useful for performing a type-safe conversion from a game object to a node.
 */
inline fun <reified T : Node> IGameObject.adapt(sender: SpaceChannel): T {
  return adapt(SpaceChannelModelContext(sender), T::class)
}

/**
 * Creates a [Node] instance from the [IGameObject] for the current context.
 *
 * This is useful for performing a type-safe conversion from a game object to a node.
 */
context(IModelContext)
inline fun <reified T : Node> IGameObject.adapt(): T {
  return adapt(this@IModelContext, T::class)
}

/**
 * Returns a single data unit [T] from the [IGameObject] for the specified [context].
 */
inline fun <reified T : IModelConstructor> IGameObject.adaptSingle(context: IModelContext): T {
  @Suppress("UNCHECKED_CAST")
  val provider = requireNotNull(models[T::class as KClass<out IModelConstructor>]) { "No ${T::class} in $this" }
  return provider.provide(this, context) as T
}

/**
 * Returns a single data unit [T] from the [IGameObject] for the current space channel.
 */
context(SpaceChannel)
inline fun <reified T : IModelConstructor> IGameObject.adaptSingle(): T {
  return adaptSingle(SpaceChannelModelContext(this@SpaceChannel))
}

/**
 * Returns a single data unit [T] from the [IGameObject] for the current context.
 */
context(IModelContext)
inline fun <reified T : IModelConstructor> IGameObject.adaptSingle(): T {
  return adaptSingle(this@IModelContext)
}
