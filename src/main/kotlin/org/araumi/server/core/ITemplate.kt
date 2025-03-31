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
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import org.araumi.server.extensions.kotlinClass
import org.araumi.server.net.command.ProtocolClass

interface ITemplate

@get:JvmName("KClass_IClass_protocolId")
val KClass<out ITemplate>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolClass>()) { "$this has no @ProtocolClass annotation" }.id

val KClass<out ITemplate>.models: List<KClass<out IModelConstructor>>
  get() {
    return memberProperties.map { it.returnType.kotlinClass }.filter { it.isSubclassOf(IModelConstructor::class) } as List<KClass<out IModelConstructor>>
  }
