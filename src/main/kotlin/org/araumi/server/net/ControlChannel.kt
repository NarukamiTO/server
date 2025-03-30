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
import io.netty.buffer.ByteBufAllocator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.araumi.server.net.command.*
import org.araumi.server.net.session.Session
import org.araumi.server.net.session.SessionHash
import org.araumi.server.protocol.OptionalMap
import org.araumi.server.protocol.ProtocolBuffer
import org.araumi.server.protocol.getTypedCodec

/**
 * All sessions have a single control channel, persisting for the entire session.
 * It is used to set up session hash, encryption algorithm, open new spaces and for client-to-server logging.
 *
 * Closing the control channel will close all spaces on the client, and basically crash the game.
 */
class ControlChannel(socket: ISocketClient) : ChannelKind(socket) {
  private val logger = KotlinLogging.logger { }

  private val commandCodec = protocol.getTypedCodec<ControlCommand>()

  override fun process(buffer: ProtocolBuffer) {
    var commandIndex = 0
    while(buffer.data.readableBytes() > 0) {
      logger.trace { "Decoding command #$commandIndex" }
      commandIndex++

      val command = commandCodec.decode(buffer)
      logger.debug { "Decoded $command" }

      when(command) {
        is HashRequestCommand -> {
          val hash = SessionHash.random()

          session = Session(hash)
          logger.debug { "Created $session" }

          sendBatch {
            val serverHeaders = mapOf(
              "server" to "Araumi TO:AGPLv3+"
            )

            for((key, value) in serverHeaders) {
              val header = "$key=$value".toByteArray()
              check(header.size <= SessionHash.HASH_LENGTH) { "Header \"$key=$value\" must be at most ${SessionHash.HASH_LENGTH} bytes, but was ${header.size} bytes" }
              val paddedHeader = SessionHash(header + ByteArray(SessionHash.HASH_LENGTH - header.size))
              HashResponseCommand(paddedHeader, channelProtectionEnabled = false).enqueue()
            }

            HashResponseCommand(hash, channelProtectionEnabled = false).enqueue()
            OpenSpaceCommand(spaceId = 0xaa55).enqueue()
          }
        }

        is InitSpaceCommand   -> {
          socket.kind = SpaceChannel(socket)
          GlobalScope.launch {
            (socket.kind as SpaceChannel).init()
          }

          logger.info { "Initialized $socket as space channel" }
        }

        else                  -> TODO("Unknown command: $command")
      }
    }

    if(buffer.data.readableBytes() > 0) {
      logger.warn { "Buffer has ${buffer.data.readableBytes()} bytes left" }
    }
  }

  fun send(command: ControlCommand) {
    val buffer = ProtocolBuffer(ByteBufAllocator.DEFAULT.buffer(), OptionalMap())
    commandCodec.encode(buffer, command)

    logger.trace { "Sending command $command" }
    socket.send(buffer)
  }

  fun sendBatch(block: ControlChannelOutgoingBatch.() -> Unit) {
    val batch = ControlChannelOutgoingBatch()
    block(batch)

    val buffer = ProtocolBuffer(ByteBufAllocator.DEFAULT.buffer(), OptionalMap())

    logger.trace { "Encoding batch with ${batch.commands.size} commands" }
    for(command in batch.commands) {
      logger.trace { "Encoding $command" }
      commandCodec.encode(buffer, command)
    }

    logger.trace { "Sending batch with ${batch.commands.size} commands" }
    socket.send(buffer)
  }
}

class ControlChannelOutgoingBatch {
  val commands: MutableList<ControlCommand> = mutableListOf()

  fun ControlCommand.enqueue() {
    commands.add(this)
  }
}
