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

import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.net.command.SpaceCommandHeader
import jp.assasans.narukami.server.protocol.ICodec
import jp.assasans.narukami.server.protocol.ProtocolBuffer
import jp.assasans.narukami.server.protocol.getTypedCodec

class SpaceEventProcessor {
  private val logger = KotlinLogging.logger { }

  private val serverEvents: Map<Long, KClass<out IServerEvent>> = ClassGraph()
    .enableAllInfo()
    .acceptPackages("jp.assasans.narukami.server")
    .scan()
    .use { scanResult ->
      scanResult.getClassesImplementing(IServerEvent::class.java).associate { classInfo ->
        @Suppress("UNCHECKED_CAST")
        val clazz = classInfo.loadClass().kotlin as KClass<out IServerEvent>

        logger.info { "Discovered server event: $clazz" }
        Pair(clazz.protocolId, clazz)
      }
    }

  fun getClass(methodId: Long): KClass<out IServerEvent>? {
    return serverEvents[methodId]
  }
}

/**
 * A client connection to a space. One space can have multiple clients (space channels).
 */
class SpaceChannel(
  socket: ISocketClient,
  val space: ISpace
) : ChannelKind(socket), KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val spaceEventProcessor: SpaceEventProcessor by inject()
  val eventScheduler: IEventScheduler by inject()

  private val commandHeaderCodec = protocol.getTypedCodec<SpaceCommandHeader>()

  suspend fun init() {
    ChannelAddedEvent(this).schedule(space.rootObject)
  }

  override suspend fun process(buffer: ProtocolBuffer) {
    var commandIndex = 0
    while(buffer.data.readableBytes() > 0) {
      logger.trace { "Processing command #$commandIndex" }
      commandIndex++

      val command = commandHeaderCodec.decode(buffer)
      logger.trace { "Received $command" }

      val eventClass = spaceEventProcessor.getClass(command.methodId)
      if(eventClass == null) {
        logger.error { "Unknown method: $command" }
        return
      }

      @Suppress("UNCHECKED_CAST")
      val codec = protocol.getCodec(eventClass.createType()) as ICodec<IServerEvent>
      val event = codec.decode(buffer)
      logger.info { "Received server event: $event" }

      val gameObject = space.objects.get(command.objectId) ?: error("Game object ${command.objectId} not found")

      // I don't know why, but using [handle] instead of [schedule] here makes some deadlock warnings not appear.
      // It is safe to assume that [IChannelKind.process] methods must not block.
      eventScheduler.schedule(event, this, gameObject)
    }

    if(buffer.data.readableBytes() > 0) {
      logger.warn { "Buffer has ${buffer.data.readableBytes()} bytes left" }
    }
  }

  /**
   * Sends a batch of space commands to the client.
   *
   * Note: This is a low-level API, most of the time you should use [IEvent.schedule] instead.
   */
  inline fun sendBatched(block: SpaceChannelOutgoingBatch.() -> Unit) {
    sendBatched(SpaceChannelOutgoingBatch().apply(block))
  }

  /**
   * Sends a batch of space commands to the client.
   *
   * Note: This is a low-level API, most of the time you should use [IEvent.schedule] instead.
   */
  fun sendBatched(batch: SpaceChannelOutgoingBatch) {
    val buffer = ProtocolBuffer.default()

    logger.trace { "Encoding batch with ${batch.commands.size} commands" }
    for(command in batch.commands) {
      logger.trace { "Encoding $command" }

      commandHeaderCodec.encode(buffer, command.header)

      @Suppress("UNCHECKED_CAST")
      val bodyCodec = protocol.getCodec(command.body::class.createType()) as ICodec<Any>
      bodyCodec.encode(buffer, command.body)
    }

    logger.trace { "Sending batch with ${batch.commands.size} commands" }
    socket.send(buffer)
  }

  suspend fun close() {
    socket.close()
  }
}

class SpaceChannelOutgoingBatch {
  val commands: MutableList<SpaceCommand> = mutableListOf()

  fun SpaceCommand.enqueue() {
    commands.add(this)
  }
}
