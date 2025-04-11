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
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.*

class ModelData private constructor(
  val id: Long,
  val objectId: Long?,
  val data: IModelConstructor?
) {
  companion object {
    fun newObject(objectId: Long): ModelData {
      return ModelData(0, objectId, null)
    }

    fun newModel(id: Long, data: IModelConstructor): ModelData {
      return ModelData(id, null, data)
    }
  }

  override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(javaClass != other?.javaClass) return false

    other as ModelData

    if(id != other.id) return false
    if(objectId != other.objectId) return false
    if(data != other.data) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + (objectId?.hashCode() ?: 0)
    result = 31 * result + (data?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return if(id == 0L) {
      "ModelData::Object(objectId=$objectId)"
    } else {
      "ModelData::Model(id=$id, data=$data)"
    }
  }
}

class ModelDataCodec : Codec<ModelData>() {
  private lateinit var longCodec: ICodec<Long>

  override fun init(protocol: IProtocol) {
    super.init(protocol)
    longCodec = protocol.getTypedCodec<Long>()
  }

  override fun encode(buffer: ProtocolBuffer, value: ModelData) {
    longCodec.encode(buffer, value.id)
    if(value.id == 0L) {
      // New object definition
      requireNotNull(value.objectId)
      longCodec.encode(buffer, value.objectId)
    } else {
      // Object model, can only follow after object definition
      requireNotNull(value.data)
      (protocol.getCodec(value.data::class.createType()) as ICodec<IModelConstructor>).encode(buffer, value.data)
    }
  }

  override fun decode(buffer: ProtocolBuffer): ModelData {
    TODO("Unsupported")
  }
}
