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

import java.net.InetSocketAddress
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.netty.*
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import jp.assasans.narukami.server.extensions.toHexString
import jp.assasans.narukami.server.protocol.ProtocolBufferCodec

class GameServer {
  private val logger = KotlinLogging.logger { }

  suspend fun start() {
    val port = 5190
    val bossGroup = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()

    try {
      val bootstrap = ServerBootstrap()
      bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel::class.java)
        .localAddress(InetSocketAddress(port))
        .childHandler(object : ChannelInitializer<SocketChannel>() {
          override fun initChannel(channel: SocketChannel) {
            // TODO: Probably should use a different coroutine context / dispatcher
            val socket = NettySocketClient(channel, CoroutineScope(Dispatchers.IO))

            channel.pipeline().addLast("decoder", ProtocolDecoder(socket))
          }
        })

      val channelFuture = bootstrap.bind().sync()
      logger.info { "Server started and listening on ${channelFuture.channel().localAddress()}" }

      // Wait until the server socket is closed
      channelFuture.channel().closeFuture().sync()
    } finally {
      // Shut down all event loops to terminate all threads
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }
}

class ProtocolDecoder(
  private val socket: NettySocketClient
) : ByteToMessageDecoder() {
  private val logger = KotlinLogging.logger { }

  private val codec = ProtocolBufferCodec()
  private val buffer: ByteBuf = ByteBufAllocator.DEFAULT.buffer()

  private val policyFileRequest = "<policy-file-request/>\u0000".toByteArray()

  @ExperimentalStdlibApi
  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
    logger.trace { "Received: ${input.toHexString()}" }
    buffer.writeBytes(input)

    // Adobe Flash cross-domain policy request
    if(buffer.readableBytes() >= policyFileRequest.size) {
      buffer.markReaderIndex()

      val request = ByteArray(policyFileRequest.size)
      buffer.readBytes(request)

      if(request.contentEquals(policyFileRequest)) {
        buffer.discardReadBytes()

        socket.launch {
          logger.info { "Responded with Adobe Flash cross-domain policy" }
          ctx.writeAndFlush(Unpooled.wrappedBuffer(buildString {
            appendLine("<?xml version=\"1.0\"?>")
            appendLine("<cross-domain-policy>")
            appendLine("<allow-access-from domain=\"*\" to-ports=\"*\"/>")
            appendLine("</cross-domain-policy>")
          }.toByteArray())).suspendAwait()
          socket.close()
        }
        return
      } else {
        buffer.resetReaderIndex()
      }
    }

    var packetIndex = 0
    while(true) {
      logger.trace { "Decoding packet #$packetIndex in network buffer" }
      logger.trace { "Network buffer: ${buffer.toHexString()}" }
      packetIndex++

      buffer.readerIndex(0)
      val packetLength = codec.getPacketLength(buffer)
      if(packetLength == null) {
        logger.trace { "Not enough data to decode packet length (${buffer.readableBytes()}), waiting for more..." }
        return
      }

      if(buffer.readableBytes() < packetLength) {
        logger.trace { "Not enough data to decode packet (${buffer.readableBytes()} / $packetLength), waiting for more..." }
        return
      }

      val protocolBuffer = codec.decode(buffer, packetLength)
      buffer.discardReadBytes()
      logger.trace { "Optional map: ${protocolBuffer.optionalMap.data.toHexString()}" }
      logger.trace { "Data: ${protocolBuffer.data.toHexString()}" }

      socket.process(protocolBuffer)
    }
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    super.channelInactive(ctx)

    logger.debug { "Channel for $socket inactive, releasing buffer" }
    buffer.release()
  }
}
