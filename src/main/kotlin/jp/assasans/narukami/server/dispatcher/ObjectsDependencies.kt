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

package jp.assasans.narukami.server.dispatcher

import kotlin.reflect.full.createType
import jp.assasans.narukami.server.core.IGameClass
import jp.assasans.narukami.server.core.protocolId
import jp.assasans.narukami.server.extensions.hasInheritedAnnotation
import jp.assasans.narukami.server.net.command.ProtocolTransient
import jp.assasans.narukami.server.protocol.Codec
import jp.assasans.narukami.server.protocol.IProtocol
import jp.assasans.narukami.server.protocol.ProtocolBuffer
import jp.assasans.narukami.server.res.Resource
import jp.assasans.narukami.server.res.isLazy
import jp.assasans.narukami.server.res.type

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
) {
  companion object {
    fun new(callbackId: Int, classes: List<IGameClass>, resources: List<Resource<*, *>>): ObjectsDependencies {
      return ObjectsDependencies(
        callbackId,
        classes = classes,
        resources = resources.map { resource ->
          ResourceDependency(
            id = resource.id.id,
            version = resource.id.version,
            kind = resource.type.type.id,
            lazy = resource.laziness.isLazy,
            dependents = listOf()
          )
        }
      )
    }
  }
}

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

      val models = gameClass.models.filterNot { it.hasInheritedAnnotation<ProtocolTransient>() }

      intCodec.encode(buffer, models.size)
      for(model in models) {
        longCodec.encode(buffer, model.protocolId)
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
