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

package jp.assasans.narukami.server.battlefield.replay

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import jp.assasans.narukami.server.net.ControlChannel
import jp.assasans.narukami.server.net.IChannelKind
import jp.assasans.narukami.server.net.ISocketClient
import jp.assasans.narukami.server.net.session.ISession
import jp.assasans.narukami.server.protocol.IProtocol
import jp.assasans.narukami.server.protocol.Protocol
import jp.assasans.narukami.server.protocol.ProtocolBuffer

class ReplaySocketClient(
  override var session: ISession?,
  private val scope: CoroutineScope,
) : ISocketClient,
    CoroutineScope by scope {
  private val logger = KotlinLogging.logger { }

  override val protocol: IProtocol = Protocol(this)
  override var kind: IChannelKind = ControlChannel(this)

  override fun process(buffer: ProtocolBuffer) {
    logger.warn { "process() stub" }
  }

  override fun send(buffer: ProtocolBuffer) {
    logger.warn { "send() stub" }
  }

  override suspend fun close() {
    logger.warn { "close() stub" }
  }
}
