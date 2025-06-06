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

package jp.assasans.narukami.server.net

import jp.assasans.narukami.server.net.session.ISession
import jp.assasans.narukami.server.protocol.IProtocol
import jp.assasans.narukami.server.protocol.ProtocolBuffer

sealed interface IChannelKind {
  val socket: ISocketClient

  suspend fun process(buffer: ProtocolBuffer)

  suspend fun close()
}

val IChannelKind.protocol: IProtocol
  get() = socket.protocol

var IChannelKind.session: ISession?
  get() = socket.session
  set(value) {
    socket.session = value
  }

val IChannelKind.sessionNotNull: ISession
  get() = checkNotNull(socket.session) { "Session is null for $socket" }

abstract class ChannelKind(
  override val socket: ISocketClient
) : IChannelKind
