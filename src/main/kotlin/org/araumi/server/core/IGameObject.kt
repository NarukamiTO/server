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

package org.araumi.server.core

import kotlin.reflect.KClass
import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.core.impl.TemplatedGameClass
import org.araumi.server.extensions.kotlinClass

interface IGameObject<TClass : IGameClass> {
  val id: Long
  val parent: TClass
  val models: Map<KClass<out IModelConstructor>, IModelConstructor>
}

fun <T : ITemplate> IGameObject<TemplatedGameClass<T>>.adapt(): T {
  val logger = KotlinLogging.logger { }
  logger.debug { "Adapting ${this.models}" }

  val templateClass = parent.template
  val template = templateClass.constructors.first().callBy(
    templateClass.constructors.first().parameters.associateWith { parameter ->
      models[parameter.type.kotlinClass] ?: error("Missing model for parameter $parameter")
    }
  )

  return template
}
