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

package org.araumi.server.protocol.factory

import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import org.araumi.server.extensions.kotlinClass
import org.araumi.server.protocol.ICodec
import org.araumi.server.protocol.IProtocol
import org.araumi.server.protocol.primitive.ResourceCodec
import org.araumi.server.res.Resource

class ResourceCodecFactory : ICodecFactory<Resource<*, *>> {
  override fun create(protocol: IProtocol, type: KType): ICodec<Resource<*, *>>? {
    if(!type.kotlinClass.isSubclassOf(Resource::class)) return null
    assert(!type.isMarkedNullable)

    val elementInfo = requireNotNull(type.arguments.first().type) { "Invalid Resource<T, _> generic argument" }
    return ResourceCodec(elementInfo)
  }
}
