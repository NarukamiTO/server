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

package org.araumi.server.core.impl

import kotlin.reflect.KClass
import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.core.*

class TransientGameObject<TClass : IGameClass>(
  override val id: Long,
  override val parent: TClass
) : IGameObject<TClass> {
  companion object {
    private val logger = KotlinLogging.logger { }

    fun <T : ITemplate> instantiate(id: Long, parent: TemplatedGameClass<T>, template: T): TransientGameObject<TemplatedGameClass<T>> {
      val gameObject = TransientGameObject(id, parent)

      parent.models.forEach { modelId ->
        try {
          val (property, clazz) = template::class.models.entries.single { it.value.protocolId == modelId }
          val provider = when(val value = property.call(template)) {
            is IModelProvider<*> -> value
            is IModelConstructor -> StaticModelProvider(value)
            else                 -> throw IllegalArgumentException("Model $modelId is not a valid model or model provider")
          }

          gameObject.models[clazz] = provider

          logger.trace { "Instantiated $provider from $template" }
        } catch(exception: Exception) {
          logger.error(exception) { "Failed to instantiate model $modelId from template $template" }
        }
      }

      return gameObject
    }
  }

  override val models: MutableMap<KClass<out IModelConstructor>, IModelProvider<*>> = mutableMapOf()

  override fun toString(): String {
    return "TransientGameObject(id=$id, parent=$parent, models=$models)"
  }
}
