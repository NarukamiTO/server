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
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import jp.assasans.narukami.server.core.IDataUnit
import jp.assasans.narukami.server.protocol.ProtocolModel
import jp.assasans.narukami.server.protocol.ProtocolStruct
import jp.assasans.narukami.server.res.Resource

/**
 * A model constructor (called *CC* in the client, probably for *client constructor*),
 * coupled with a model info and model resources.
 *
 * @see jp.assasans.narukami.server.core.ArchitectureDocs
 */
@ProtocolStruct
interface IModelConstructor : IDataUnit {
  /**
   * Returns a list of resources to load with this model.
   */
  fun getResources(): List<Resource<*, *>> {
    return collectResources(this)
  }
}

@get:JvmName("KClass_IModelConstructor_protocolId")
val KClass<out IModelConstructor>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolModel>()) { "$this has no @ProtocolModel annotation" }.id

fun collectResources(root: Any): List<Resource<*, *>> {
  fun collect(instance: Any?, visited: MutableSet<Any> = mutableSetOf()): List<Resource<*, *>> {
    if(instance == null || instance in visited) return emptyList()
    visited.add(instance)

    return instance::class.declaredMemberProperties.flatMap { property ->
      val value = property.getter.call(instance)
      when {
        value is Resource<*, *>                                       -> listOf(value)
        value != null && value::class.hasAnnotation<ProtocolStruct>() -> collect(value, visited)
        else                                                          -> emptyList()
      }
    }
  }

  return collect(root)
}
