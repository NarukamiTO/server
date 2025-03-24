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

import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import kotlinx.coroutines.*
import org.araumi.server.net.command.ControlCommand
import org.araumi.server.protocol.Protocol
import org.araumi.server.protocol.ProtocolBuffer
import org.araumi.server.protocol.ProtocolBufferCodec
import org.araumi.server.protocol.getTypedCodec

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
            val socket = NettySocketClient(channel)

            channel.pipeline().addLast("decoder", ProtocolDecoder(socket))
            // channel.pipeline().addLast("encoder", ProtocolEncoder())
            // channel.pipeline().addLast(CoroutineServerHandler())
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

class ProtocolDecoder(val socket: NettySocketClient) : ByteToMessageDecoder() {
  private val logger = KotlinLogging.logger { }

  private val codec = ProtocolBufferCodec()
  private val buffer: ByteBuf = ByteBufAllocator.DEFAULT.buffer()

  @ExperimentalStdlibApi
  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
    logger.trace { "Received: ${input.toHexString()}" }
    buffer.writeBytes(input)

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

    // val bytes = ByteArray(input.readableBytes())
    // input.readBytes(bytes)
    //
    // val protocol = Protocol()
    // // val array = ByteArray(input.readableBytes())
    // // input.readBytes(array)
    // // println("data: ${array.decodeToString()}")
    // val buffer = ProtocolBufferCodec().decode(input)
    // val command = protocol.getTypedCodec<ControlCommand>().decode(buffer)
    // println("decoded $command")
    //
    // when(command) {
    //   is HashRequestCommand -> {
    //     val outBuffer = ProtocolBuffer(ByteBufAllocator.DEFAULT.buffer(), OptionalMap())
    //     protocol.getTypedCodec<ControlCommand>().encode(outBuffer, HashResponseCommand(
    //       hash = ByteArray(32) { 0xff.toByte() },
    //       channelProtectionEnabled = false
    //     ))
    //     protocol.getTypedCodec<ControlCommand>().encode(outBuffer, OpenSpaceCommand(
    //       spaceId = 0xaa55
    //     ))
    //
    //     out.add(outBuffer)
    //   }
    //
    //   else                  -> TODO("Unknown command: $command")
    // }

    // val data = ByteArray(buffer.data.readableBytes())
    // buffer.data.readBytes(data)
    // logger.info { "Encoded: ${data.toHexString()}" }
  }
}

fun ByteBuf.toHexString(): String {
  val builder = StringBuilder()
  val readerIndex = this.readerIndex()
  for(index in 0 until this.readableBytes()) {
    val byte = this.readByte()
    builder.append(String.format("%02X", byte))
  }
  this.readerIndex(readerIndex)

  return builder.toString()
}

class ProtocolEncoder : MessageToByteEncoder<ProtocolBuffer>() {
  override fun encode(ctx: ChannelHandlerContext, buffer: ProtocolBuffer, out: ByteBuf) {
    val protocol = Protocol()
    val codec = ProtocolBufferCodec()

    val outBuffer = ByteBufAllocator.DEFAULT.buffer()
    codec.encode(outBuffer, buffer)
    println("response hex: ${outBuffer.toHexString()}")

    out.writeBytes(outBuffer.copy())

    val packetLength = checkNotNull(codec.getPacketLength(outBuffer)) { "Not enough data to decode packet length" }
    val buffer2 = codec.decode(outBuffer, packetLength)
    val command2 = protocol.getTypedCodec<ControlCommand>().decode(buffer2)
    println("decoded loopback $command2")
  }
}

class CoroutineServerHandler : ChannelInboundHandlerAdapter(), CoroutineScope {
  private val job = SupervisorJob()
  override val coroutineContext: CoroutineContext
    get() = Dispatchers.Default + job

  override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
    launch {
      // Switch back to event loop thread to respond
      withContext(ctx.executor().asCoroutineDispatcher()) {
        ctx.writeAndFlush(message).sync()
      }
    }
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace()
    ctx.close()
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    // Cancel all coroutines when the channel is closed
    job.cancel()
    super.channelInactive(ctx)
  }
}
