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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.netty.*
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.socket.SocketChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.launch
import jp.assasans.narukami.server.extensions.toHexString
import jp.assasans.narukami.server.net.session.ISession
import jp.assasans.narukami.server.protocol.IProtocol
import jp.assasans.narukami.server.protocol.Protocol
import jp.assasans.narukami.server.protocol.ProtocolBuffer
import jp.assasans.narukami.server.protocol.ProtocolBufferCodec

interface ISocketClient : CoroutineScope {
  val protocol: IProtocol
  var kind: IChannelKind
  var session: ISession?

  fun process(buffer: ProtocolBuffer)
  fun send(buffer: ProtocolBuffer)

  suspend fun close()
}

class NettySocketClient(
  val channel: SocketChannel,
  private val scope: CoroutineScope
) : ISocketClient,
    CoroutineScope by scope {
  private val logger = KotlinLogging.logger { }

  override val protocol: IProtocol = Protocol()
  override var kind: IChannelKind = ControlChannel(this)
  override var session: ISession? = null

  private val codec = ProtocolBufferCodec()

  private val receiveQueue = Channel<ProtocolBuffer>(Channel.UNLIMITED)
  private val readerJob = scope.launch {
    for(buffer in receiveQueue) {
      try {
        processDirect(buffer)
      } catch(exception: Exception) {
        logger.error(exception) { "Error processing buffer $buffer" }
      }
    }
  }

  private val sendQueue = Channel<ProtocolBuffer>(Channel.UNLIMITED)
  private val writerJob = scope.launch {
    for(buffer in sendQueue) {
      try {
        sendDirect(buffer)
      } catch(exception: Exception) {
        logger.error(exception) { "Error sending buffer $buffer" }
      }
    }
  }

  private suspend fun sendDirect(buffer: ProtocolBuffer) {
    logger.trace { "Sending ${buffer.data.readableBytes()} data bytes" }

    val outBuffer = ByteBufAllocator.DEFAULT.buffer()

    codec.encode(outBuffer, buffer)
    buffer.data.release()

    logger.trace { "Outgoing hex: ${outBuffer.toHexString()}" }

    // Ownership of the buffer is transferred to Netty, no need to call release()
    channel.writeAndFlush(outBuffer).suspendAwait()
  }

  override fun send(buffer: ProtocolBuffer) {
    logger.trace { "Queueing outgoing $buffer" }
    sendQueue.trySend(buffer).onFailure {
      logger.error(it) { "Failed to enqueue outgoing buffer $buffer" }
    }
  }

  override fun process(buffer: ProtocolBuffer) {
    logger.trace { "Queueing incoming $buffer" }
    receiveQueue.trySend(buffer).onFailure {
      logger.error(it) { "Failed to enqueue incoming buffer $buffer" }
    }
  }

  private suspend fun processDirect(buffer: ProtocolBuffer) {
    logger.trace { "Processing as $kind" }
    kind.process(buffer)
  }

  override suspend fun close() {
    logger.debug { "Closing socket $this" }

    receiveQueue.close()
    sendQueue.close()

    readerJob.cancelAndJoin()
    writerJob.cancelAndJoin()

    channel.close().suspendAwait()
  }
}
