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

data class TemplatedGameClass<T : ITemplate>(
  override val id: Long,
  override val models: List<KClass<out IModelConstructor>>,
  val template: KClass<T>,
) : IGameClass {
  private val logger = KotlinLogging.logger { }

  companion object {
    fun <T : ITemplate> fromTemplate(template: KClass<T>): TemplatedGameClass<T> {
      return TemplatedGameClass(
        id = template.protocolId,
        models = template.models.values.toList(),
        template = template
      )
    }
  }
}
