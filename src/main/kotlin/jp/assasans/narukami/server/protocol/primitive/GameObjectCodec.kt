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

package jp.assasans.narukami.server.protocol.primitive

import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.protocol.*

class GameObjectCodec : Codec<IGameObject>() {
  private lateinit var longCodec: ICodec<Long>

  override fun init(protocol: IProtocol) {
    super.init(protocol)
    longCodec = protocol.getTypedCodec<Long>()
  }

  override fun encode(buffer: ProtocolBuffer, value: IGameObject) {
    longCodec.encode(buffer, value.id)
  }

  override fun decode(buffer: ProtocolBuffer): IGameObject {
    val channel = protocol.socket.kind
    if(channel !is SpaceChannel) throw IllegalStateException("GameObjectCodec can only be used in space channel")

    val id = longCodec.decode(buffer)
    return channel.space.objects.get(id) ?: throw IllegalStateException("Object with ID $id not found in space ${channel.space.id}")
  }
}
