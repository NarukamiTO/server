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
import kotlin.reflect.full.memberProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.core.*
import org.araumi.server.extensions.kotlinClass

class TransientGameObject<TClass : IGameClass>(
  override val id: Long,
  override val parent: TClass
) : IGameObject<TClass> {
  companion object {
    private val logger = KotlinLogging.logger { }

    fun <T : ITemplate> instantiate(id: Long, parent: TemplatedGameClass<T>, template: T): TransientGameObject<TemplatedGameClass<T>> {
      val gameObject = TransientGameObject(id, parent)

      parent.models.forEach { modelId ->
        val modelClass = template::class.models.first { it.protocolId == modelId }
        val model = template::class.memberProperties.first { it.returnType.kotlinClass == modelClass }
          .getter.call(template) as IModelConstructor
        gameObject.models[modelClass] = model

        logger.debug { "Instantiated $model from $template" }
      }

      return gameObject
    }
  }

  override val models: MutableMap<KClass<out IModelConstructor>, IModelConstructor> = mutableMapOf()
}
