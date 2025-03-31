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

import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBufAllocator
import org.araumi.server.core.*
import org.araumi.server.core.impl.EventScheduler
import org.araumi.server.dispatcher.DispatcherModelLoadObjectsManagedEvent
import org.araumi.server.entrance.*
import org.araumi.server.net.command.ProtocolClass
import org.araumi.server.net.command.SpaceCommandHeader
import org.araumi.server.protocol.ICodec
import org.araumi.server.protocol.OptionalMap
import org.araumi.server.protocol.ProtocolBuffer
import org.araumi.server.protocol.getTypedCodec
import org.koin.core.component.KoinComponent

/**
 * Loads contained in the event resources on the client before scheduling the actual event.
 *
 * Used when client event contains resources that need to be loaded beforehand,
 * e.g. in [org.araumi.server.entrance.EntranceAlertModelShowAlertEvent].
 */
data class PreloadResourcesWrappedEvent<T : IEvent>(
  val event: T
) : IEvent

@ProtocolClass(2)
data class EntranceTemplate(
  val entrance: EntranceModelCC,
  val captcha: CaptchaModelCC,
  val login: LoginModelCC,
  val registration: RegistrationModelCC,
  val entranceAlert: EntranceAlertModelCC,
) : ITemplate

/**
 * A client connection to a space. One space can have multiple clients (space channels).
 */
class SpaceChannel(
  socket: ISocketClient,
  val space: ISpace
) : ChannelKind(socket), KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val commandHeaderCodec = protocol.getTypedCodec<SpaceCommandHeader>()

  val eventScheduler: EventScheduler = EventScheduler()

  suspend fun init() {
    val entranceObject = space.objects.get(2) ?: error("Entrance object not found")
    DispatcherModelLoadObjectsManagedEvent(
      objects = listOf(entranceObject),
    ).schedule(space.rootObject).await()
  }

  override fun process(buffer: ProtocolBuffer) {
    var commandIndex = 0
    while(buffer.data.readableBytes() > 0) {
      logger.trace { "Processing command #$commandIndex" }
      commandIndex++

      val command = commandHeaderCodec.decode(buffer)
      logger.trace { "Received $command" }

      val serverEvents = mutableMapOf<Long, KClass<out IServerEvent>>()
      ClassGraph().enableAllInfo().acceptPackages("org.araumi").scan().use { scanResult ->
        @Suppress("UNCHECKED_CAST")
        val classes = scanResult
          .getClassesImplementing(IServerEvent::class.qualifiedName)
          .loadClasses()
          .map { it.kotlin } as List<KClass<out IServerEvent>>
        logger.info { "Found ${classes.size} server events" }

        for(clazz in classes) {
          logger.debug { "Discovered server event: $clazz" }
          serverEvents[clazz.protocolId] = clazz
        }
      }

      val eventClass = serverEvents[command.methodId]
      if(eventClass == null) {
        logger.error { "Unknown method: $command" }
        return
      }

      @Suppress("UNCHECKED_CAST")
      val codec = protocol.getCodec(eventClass.createType()) as ICodec<IServerEvent>
      val event = codec.decode(buffer)
      logger.info { "Received server event: $event" }

      // val gameObject = TransientGameObject(command.objectId, TransientGameClass(id = -1, models = listOf()))
      val gameObject = space.objects.get(command.objectId) ?: error("Game object ${command.objectId} not found")

      eventScheduler.processServerEvent(event, this, gameObject)
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
    val buffer = ProtocolBuffer(ByteBufAllocator.DEFAULT.buffer(), OptionalMap())

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
}

class SpaceChannelOutgoingBatch {
  val commands: MutableList<SpaceCommand> = mutableListOf()

  fun SpaceCommand.enqueue() {
    commands.add(this)
  }
}
