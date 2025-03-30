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

package org.araumi.server.dispatcher

import kotlin.reflect.full.createType
import org.araumi.server.net.IGameClass
import org.araumi.server.protocol.Codec
import org.araumi.server.protocol.IProtocol
import org.araumi.server.protocol.ProtocolBuffer

data class ResourceDependency(
  val id: Long,
  val version: Long,
  val kind: Short,
  val lazy: Boolean,
  val dependents: List<Long>
)

data class ObjectsDependencies(
  val callbackId: Int,
  val classes: List<IGameClass>,
  val resources: List<ResourceDependency>,
)

class ObjectsDependenciesCodec : Codec<ObjectsDependencies>() {
  private lateinit var byteCodec: Codec<Byte>
  private lateinit var intCodec: Codec<Int>
  private lateinit var longCodec: Codec<Long>
  private lateinit var shortCodec: Codec<Short>
  private lateinit var booleanCodec: Codec<Boolean>

  override fun init(protocol: IProtocol) {
    super.init(protocol)
    byteCodec = protocol.getCodec(Byte::class.createType()) as Codec<Byte>
    intCodec = protocol.getCodec(Int::class.createType()) as Codec<Int>
    longCodec = protocol.getCodec(Long::class.createType()) as Codec<Long>
    shortCodec = protocol.getCodec(Short::class.createType()) as Codec<Short>
    booleanCodec = protocol.getCodec(Boolean::class.createType()) as Codec<Boolean>
  }

  override fun encode(buffer: ProtocolBuffer, value: ObjectsDependencies) {
    intCodec.encode(buffer, value.callbackId)

    intCodec.encode(buffer, value.classes.size)
    for(gameClass in value.classes) {
      longCodec.encode(buffer, gameClass.id)

      intCodec.encode(buffer, gameClass.models.size)
      for(model in gameClass.models) {
        longCodec.encode(buffer, model)
      }
    }

    intCodec.encode(buffer, value.resources.size)
    for(resource in value.resources) {
      longCodec.encode(buffer, resource.id)
      shortCodec.encode(buffer, resource.kind)
      longCodec.encode(buffer, resource.version)
      booleanCodec.encode(buffer, resource.lazy)

      byteCodec.encode(buffer, resource.dependents.size.toByte())
      for(dependent in resource.dependents) {
        longCodec.encode(buffer, dependent)
      }
    }
  }

  override fun decode(buffer: ProtocolBuffer): ObjectsDependencies {
    TODO("Unsupported")
  }
}
