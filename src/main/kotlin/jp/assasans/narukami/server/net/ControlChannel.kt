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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.DeferredPending
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.net.command.*
import jp.assasans.narukami.server.net.session.Session
import jp.assasans.narukami.server.net.session.SessionHash
import jp.assasans.narukami.server.protocol.ProtocolBuffer
import jp.assasans.narukami.server.protocol.getTypedCodec

/**
 * All sessions have a single control channel, persisting for the entire session.
 * It is used to set up session hash, encryption algorithm, open new spaces and for client-to-server logging.
 *
 * Closing the control channel will close all spaces on the client, and basically crash the game.
 */
class ControlChannel(socket: ISocketClient) : ChannelKind(socket), KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val spaces: IRegistry<ISpace> by inject()
  private val sessions: ISessionRegistry by inject()

  private val commandCodec = protocol.getTypedCodec<ControlCommand>()

  override suspend fun process(buffer: ProtocolBuffer) {
    var commandIndex = 0
    while(buffer.data.readableBytes() > 0) {
      logger.trace { "Decoding command #$commandIndex" }
      commandIndex++

      val command = commandCodec.decode(buffer)
      logger.debug { "Decoded $command" }

      when(command) {
        is HashRequestCommand -> {
          val hash = SessionHash.random()

          logger.info { "Hash request properties: ${command.properties}" }

          // Create a new session
          val session = Session(hash, command.properties, this)
          this.session = session
          sessions.add(session)
          logger.debug { "Created $session" }

          sendBatch {
            val serverHeaders = mapOf(
              "server" to "Narukami TO:AGPLv3+"
            )

            for((key, value) in serverHeaders) {
              val header = "$key=$value".toByteArray()
              check(header.size <= SessionHash.HASH_LENGTH) { "Header \"$key=$value\" must be at most ${SessionHash.HASH_LENGTH} bytes, but was ${header.size} bytes" }
              val paddedHeader = SessionHash(header + ByteArray(SessionHash.HASH_LENGTH - header.size))
              HashResponseCommand(paddedHeader, channelProtectionEnabled = false).enqueue()
            }

            HashResponseCommand(hash, channelProtectionEnabled = false).enqueue()
          }

          // Bootstrap the first space using low-level API, rest
          // of the spaces should be opened using the Systems API.
          // Loading continues in [EntranceSystem#channelAdded].
          openSpace(Space.stableId("entrance"))
        }

        is InitSpaceCommand   -> {
          check(this.session == null) { "Session already assigned, control channel -> space channel upgrade (unreachable)" }

          val session = sessions.get(command.hash) ?: error("Session ${command.hash} not found")
          val pendingSpace = session.pendingSpaces.take(command.spaceId)
          if(pendingSpace == null) {
            logger.error { "Space channel opened for nonexistent pending space ${command.spaceId}" }
            socket.close()
            return
          }

          val space = spaces.get(command.spaceId)
          if(space == null) {
            logger.error { "Space channel opened for nonexistent space ${pendingSpace.id}" }
            socket.close()
            return
          }

          val channel = SpaceChannel(socket, space)
          socket.kind = channel
          socket.kind.session = session
          session.spaces.add(channel)

          logger.debug { "Assigned $this to $session" }

          // Initialize the space channel and schedule [ChannelAddedEvent]
          (socket.kind as SpaceChannel).init()

          // Mark pending space as opened
          (pendingSpace as DeferredPending<SpaceChannel>).complete(channel)

          logger.info { "Initialized $socket as space channel" }
        }

        else                  -> TODO("Unknown command: $command")
      }
    }

    if(buffer.data.readableBytes() > 0) {
      logger.warn { "Buffer has ${buffer.data.readableBytes()} bytes left" }
    }
  }

  /**
   * Opens a new space channel.
   *
   * Note: This is a low-level API, most of the time you should use
   * [jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent] instead.
   */
  fun openSpace(id: Long): IPending<SpaceChannel> {
    val pending = DeferredPending<SpaceChannel>(id)
    sessionNotNull.pendingSpaces.add(pending)

    sendBatch {
      OpenSpaceCommand(id).enqueue()
    }

    return pending
  }

  fun send(command: ControlCommand) {
    val buffer = ProtocolBuffer.default()
    commandCodec.encode(buffer, command)

    logger.trace { "Sending command $command" }
    socket.send(buffer)
  }

  fun sendBatch(block: ControlChannelOutgoingBatch.() -> Unit) {
    val batch = ControlChannelOutgoingBatch()
    block(batch)

    val buffer = ProtocolBuffer.default()

    logger.trace { "Encoding batch with ${batch.commands.size} commands" }
    for(command in batch.commands) {
      logger.trace { "Encoding $command" }
      commandCodec.encode(buffer, command)
    }

    logger.trace { "Sending batch with ${batch.commands.size} commands" }
    socket.send(buffer)
  }

  override suspend fun close() {
    socket.close()
  }
}

class ControlChannelOutgoingBatch {
  val commands: MutableList<ControlCommand> = mutableListOf()

  fun ControlCommand.enqueue() {
    commands.add(this)
  }
}
