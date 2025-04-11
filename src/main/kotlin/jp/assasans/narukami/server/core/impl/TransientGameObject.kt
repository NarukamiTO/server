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
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*

class TransientGameObject(
  override val id: Long,
  override val parent: IGameClass
) : IGameObject {
  companion object {
    private val logger = KotlinLogging.logger { }

    private val lastId = AtomicLong(-21122019)

    /**
     * Generates a new transient ID for a game object.
     *
     * Transient IDs are negative, as opposed to persistent IDs, which are positive.
     */
    fun freeId(): Long {
      return lastId.getAndDecrement()
    }

    fun <T : ITemplate> instantiate(id: Long, parent: TemplatedGameClass<T>, template: T): IGameObject {
      val gameObject = TransientGameObject(id, parent)

      parent.models.forEach { clazz ->
        try {
          val (property, _) = template::class.models.entries.single { it.value == clazz }
          val provider = when(val value = property.call(template)) {
            is IModelProvider<*> -> value
            is IModelConstructor -> StaticModelProvider(value)
            else                 -> throw IllegalArgumentException("$value (for $clazz) is not a valid model or model provider")
          }

          gameObject.models[clazz] = provider
          logger.trace { "Instantiated $provider from $template" }
        } catch(exception: Exception) {
          logger.error(exception) { "Failed to instantiate model $clazz from template $template" }
        }
      }

      return gameObject
    }
  }

  override val components: MutableMap<KClass<out IComponent>, IComponent> = mutableMapOf()
  override val models: MutableMap<KClass<out IModelConstructor>, IModelProvider<*>> = mutableMapOf()

  override fun toString(): String {
    return "TransientGameObject(id=$id, parent=$parent, models=$models)"
  }
}
