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

import org.araumi.server.protocol.*

data class ObjectsData(
  val objects: List<GameObject>,
  val modelData: List<ModelData>,
)

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
      longCodec.encode(buffer, gameObject.parent)
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
