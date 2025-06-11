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

/**
 * Node is a type-safe view of a [IGameObject] for a certain [IModelContext].
 *
 * Nodes can be created manually using the [IGameObject.adapt] function.
 *
 * Nodes may define extension members to eliminate repeated code.
 */
open class Node {
  lateinit var context: IModelContext
    private set
  lateinit var gameObject: IGameObject
    private set

  fun init(context: IModelContext, gameObject: IGameObject) {
    this.context = context
    this.gameObject = gameObject
  }

  // XXX: Incorrect implementation? It ignores data units in subclasses,
  //  but it also checks javaClass, so this should be fine.
  final override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(javaClass != other?.javaClass) return false

    other as Node

    if(context != other.context) return false
    if(gameObject != other.gameObject) return false

    return true
  }

  final override fun hashCode(): Int {
    var result = context.hashCode()
    result = 31 * result + gameObject.hashCode()
    return result
  }
}

data class SingleNode<T : IDataUnit>(
  val node: T
) : Node()

val Iterable<Node>.gameObjects: List<IGameObject>
  get() = map { it.gameObject }

operator fun <T : Node> Iterable<T>.minus(gameObject: IGameObject): Iterable<T> {
  return filter { it.gameObject != gameObject }
}
