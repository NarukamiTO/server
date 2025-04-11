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

import kotlin.reflect.full.declaredMembers
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.protocolId
import jp.assasans.narukami.server.extensions.hasInheritedAnnotation
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.command.ProtocolTransient
import jp.assasans.narukami.server.protocol.*

data class ObjectsData(
  val objects: List<IGameObject>,
  val modelData: List<ModelData>,
) {
  companion object {
    fun new(objects: List<IGameObject>, sender: SpaceChannel): ObjectsData {
      return ObjectsData(
        objects = objects,
        modelData = objects.flatMap { gameObject ->
          listOf(
            ModelData.newObject(gameObject.id)
          ) + gameObject.models
            .filterNot { (clazz, _) -> clazz.hasInheritedAnnotation<ProtocolTransient>() }
            .filter { (clazz, _) -> clazz.declaredMembers.isNotEmpty() }
            .map { (clazz, model) -> ModelData.newModel(clazz.protocolId, model.provide(gameObject, sender)) }
        }
      )
    }
  }
}

class ObjectsDataCodec : Codec<ObjectsData>() {
  private lateinit var intCodec: ICodec<Int>
  private lateinit var longCodec: ICodec<Long>
  private lateinit var modelDataCodec: ICodec<ModelData>

  override fun init(protocol: IProtocol) {
    super.init(protocol)
    intCodec = protocol.getTypedCodec<Int>()
    longCodec = protocol.getTypedCodec<Long>()
    modelDataCodec = protocol.getTypedCodec<ModelData>()
  }

  override fun encode(buffer: ProtocolBuffer, value: ObjectsData) {
    intCodec.encode(buffer, value.objects.size)
    for(gameObject in value.objects) {
      longCodec.encode(buffer, gameObject.id)
      longCodec.encode(buffer, gameObject.parent.id)
    }

    intCodec.encode(buffer, value.modelData.size)
    for(modelData in value.modelData) {
      modelDataCodec.encode(buffer, modelData)
    }
  }

  override fun decode(buffer: ProtocolBuffer): ObjectsData {
    TODO("Unsupported")
  }
}
