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

package org.araumi.server.net

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.netty.*
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.socket.SocketChannel
import org.araumi.server.net.session.ISession
import org.araumi.server.protocol.IProtocol
import org.araumi.server.protocol.Protocol
import org.araumi.server.protocol.ProtocolBuffer
import org.araumi.server.protocol.ProtocolBufferCodec

interface ISocketClient {
  val protocol: IProtocol
  var kind: IChannelKind
  var session: ISession?

  fun send(buffer: ProtocolBuffer)
  suspend fun close()
}

class NettySocketClient(
  val channel: SocketChannel
) : ISocketClient {
  private val logger = KotlinLogging.logger { }

  override val protocol: IProtocol = Protocol()
  override var kind: IChannelKind = ControlChannel(this)
  override var session: ISession? = null

  fun process(buffer: ProtocolBuffer) {
    logger.trace { "Processing as $kind" }
    kind.process(buffer)
  }

  override fun send(buffer: ProtocolBuffer) {
    logger.trace { "Sending ${buffer.data.readableBytes()} bytes" }

    val outBuffer = ByteBufAllocator.DEFAULT.buffer()
    ProtocolBufferCodec().encode(outBuffer, buffer)
    logger.trace { "Outgoing hex: ${outBuffer.toHexString()}" }

    channel.writeAndFlush(outBuffer.copy()).sync()

    // val buffer2 = ProtocolBufferCodec().decode(outBuffer)
    // val command2 = protocol.getTypedCodec<ControlCommand>().decode(buffer2)
    // println("decoded loopback $command2")
  }

  override suspend fun close() {
    logger.debug { "Closing socket" }
    channel.close().suspendAwait()
  }
}
