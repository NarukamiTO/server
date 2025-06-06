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

package jp.assasans.narukami.server.core.internal

import kotlin.reflect.KClass
import jp.assasans.narukami.server.core.IComponent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.ITemplate

sealed interface TemplateMember {
  data class Model(val model: KClass<out IModelConstructor>) : TemplateMember
  data class Component(val component: KClass<out IComponent>, val nullable: Boolean) : TemplateMember
  data class Template(val template: KClass<out ITemplate>) : TemplateMember
}
