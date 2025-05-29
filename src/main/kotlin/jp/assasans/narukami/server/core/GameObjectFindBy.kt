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
import jp.assasans.narukami.server.battlefield.IGroupComponent
import jp.assasans.narukami.server.core.impl.NodeBuilder

/**
 * Finds a node of type [T] in the collection of game objects that has the same group component as the [source] node.
 *
 * @param source source node to match the group component against
 * @param group group component to match
 * @param output class of the node to search for
 * @return matching node of type [T]
 */
fun <T : Node> Iterable<IGameObject>.findBy(source: Node, group: KClass<out IGroupComponent>, output: KClass<out T>): T {
  val nodeBuilder = NodeBuilder()
  val nodeDefinition = nodeBuilder.getNodeDefinition(output.createType())
  val sourceGroup = source.gameObject.components[group] ?: throw IllegalArgumentException("No group component $group in $this")

  var single: T? = null
  var found = false
  for(gameObject in this) {
    val targetGroup = gameObject.components[group]
    if(sourceGroup != targetGroup) continue

    val node = nodeBuilder.tryBuildLazy(nodeDefinition, gameObject, source.context)
    if(node != null) {
      if(found) throw IllegalArgumentException("More than one matching node found for $output, grouped by $sourceGroup ($group)")
      found = true
      single = node as T
    }
  }

  return single ?: throw IllegalArgumentException("Failed to find node ${output.qualifiedName}, grouped by $sourceGroup ($group)")
}

/**
 * Finds a node of type [T] in the collection of game objects that has the same group component as the [source] node.
 *
 * @param source source node to match the group component against
 * @param T class of the node to search for
 * @param G group component to match
 * @return matching node of type [T]
 */
inline fun <reified T : Node, reified G : IGroupComponent> Iterable<IGameObject>.findBy(source: Node): T {
  return findBy(source, G::class, T::class)
}

/**
 * Finds a node of type [T] in the collection of game objects for each source node in [sources].
 *
 * @param sources source nodes to match the group component against
 * @param T class of the node to search for
 * @param G group component to match
 * @return matching nodes of type [T]
 */
inline fun <reified T : Node, reified G : IGroupComponent> Iterable<IGameObject>.findAllBy(sources: Iterable<Node>): List<T> {
  return sources.map { source -> findBy<T, G>(source) }
}
