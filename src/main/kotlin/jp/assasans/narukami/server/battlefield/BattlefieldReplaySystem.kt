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

package jp.assasans.narukami.server.battlefield

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.createType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBufAllocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.extensions.toHexString
import jp.assasans.narukami.server.net.*
import jp.assasans.narukami.server.net.session.ISession
import jp.assasans.narukami.server.net.session.Session
import jp.assasans.narukami.server.net.session.SessionHash
import jp.assasans.narukami.server.protocol.*

// TODO: All code in this file is pretty bad and should be refactored

const val IS_RECORDING = false

object BattlefieldReplayMiddleware : EventMiddleware {
  private val logger = KotlinLogging.logger { }

  var replayWriter: ReplayWriter? = null

  override fun process(eventScheduler: IEventScheduler, event: IEvent, gameObject: IGameObject, context: IModelContext) {
    if(!context.space.rootObject.models.contains(BattlefieldModelCC::class)) return

    if(IS_RECORDING) {
      if(replayWriter == null) {
        replayWriter = ReplayWriter(context.space)
        replayWriter!!.writeComment("replay started at ${Clock.System.now()}")
        replayWriter!!.writeComment("space id: ${context.space.id}")
        logger.info { "Replay writer initialized for space ${context.space.id}" }
      }

      if(event !is IServerEvent) return
      if(context !is SpaceChannelModelContext) return

      replayWriter!!.writeEvent(event, gameObject, context.channel)

      logger.debug { "Recorded: ${event::class.simpleName}" }
    }

    // eventScheduler.schedule(BattleDebugMessageEvent("${event::class.simpleName}"), context, context.space.rootObject)
  }
}

class FakeSocket(val space: ISpace) : ISocketClient {
  override val protocol: IProtocol
    get() = TODO("Not yet implemented")
  override var kind: IChannelKind
    get() = SpaceChannel(this, space)
    set(value) = TODO("Not yet implemented")
  override var session: ISession?
    get() = TODO("Not yet implemented")
    set(value) = TODO("Not yet implemented")

  override fun process(buffer: ProtocolBuffer) {
    TODO("Not yet implemented")
  }

  override fun send(buffer: ProtocolBuffer) {
    TODO("Not yet implemented")
  }

  override suspend fun close() {
    TODO("Not yet implemented")
  }

  override val coroutineContext: CoroutineContext
    get() = TODO("Not yet implemented")
}

class ReplayWriter(val space: ISpace) {
  private val logger = KotlinLogging.logger { }

  val writer = Files.newOutputStream(Paths.get("battle.replay"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)!!.writer()

  val protocol = Protocol(FakeSocket(space))
  val protocolBufferCodec = ProtocolBufferCodec()

  fun writeComment(comment: String) {
    writer.write("# $comment\n")
    writer.flush()
  }

  fun writeEvent(event: IServerEvent, gameObject: IGameObject, sender: SpaceChannel) {
    val codec = protocol.getCodec(event::class.createType()) as ICodec<IServerEvent>
    val buffer = ProtocolBuffer.default()
    codec.encode(buffer, event)
    val outBuffer = ByteBufAllocator.DEFAULT.buffer()
    protocolBufferCodec.encode(outBuffer, buffer)

    val columns = listOf(
      Clock.System.now().toEpochMilliseconds(),
      sender.sessionNotNull.hash.value.toHexString(),
      gameObject.id,
      event::class.protocolId.toString(),
      outBuffer.toHexString(),
    )
    writer.write(columns.joinToString(" ") + "\n")
    writer.flush()
  }
}

data class ReplayEvent(
  val timestamp: Long,
  val sender: SpaceChannel,
  val gameObject: IGameObject,
  val event: IServerEvent,
)

class ReplayReader(
  val space: ISpace,
  val userObject: IGameObject,
) : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val sessions: ISessionRegistry by inject()
  private val spaceEventProcessor: SpaceEventProcessor by inject()

  private val reader = Files.newBufferedReader(Paths.get("battle.replay"))

  val protocol = Protocol(FakeSocket(space))
  val protocolBufferCodec = ProtocolBufferCodec()

  fun readEvents(): Flow<ReplayEvent> = flow {
    var firstTimestamp: Long? = null
    var startTime: Long? = null

    reader.useLines { lines ->
      for(line in lines) {
        if(line.isBlank()) continue
        if(line.startsWith("#")) continue

        val columns = line.split(" ")
        if(columns.size != 5) {
          logger.warn { "Invalid replay line format: $line" }
          continue
        }

        try {
          val timestamp = columns[0].toLong()
          val sessionHash = SessionHash(columns[1].hexToByteArray())
          val objectId = columns[2].toLong()
          val methodId = columns[3].toLong()
          val encodedData = columns[4].hexToByteArray()

          // Initialize timing on first event
          if(firstTimestamp == null) {
            firstTimestamp = timestamp
            startTime = Clock.System.now().toEpochMilliseconds()
            // Yield first event immediately
          } else {
            // Calculate delay for subsequent events
            val deltaFromFirst = timestamp - firstTimestamp
            val targetTime = startTime!! + deltaFromFirst
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val delayMs = targetTime - currentTime

            if(delayMs > 0) {
              delay(delayMs)
            }
          }

          val inBuffer = ByteBufAllocator.DEFAULT.buffer()
          inBuffer.writeBytes(encodedData)

          val packetLength = requireNotNull(protocolBufferCodec.getPacketLength(inBuffer))
          val buffer = protocolBufferCodec.decode(inBuffer, packetLength)

          val eventClass = requireNotNull(spaceEventProcessor.getClass(methodId))
          val codec = protocol.getCodec(eventClass.createType()) as ICodec<IServerEvent>
          val event = codec.decode(buffer)

          val session = if(sessions.has(sessionHash)) {
            sessions.get(sessionHash)!!
          } else {
            val controlClient = ReplaySocketClient(null, CoroutineScope(Dispatchers.IO))
            val session = Session(sessionHash, emptyMap(), controlClient.kind as ControlChannel)
            controlClient.session = session
            session.user = userObject
            sessions.add(session)
            session
          }

          val spaceChannel = if(session.spaces.has(space.id)) {
            session.spaces.get(space.id)!!
          } else {
            val spaceClient = ReplaySocketClient(session, CoroutineScope(Dispatchers.IO))
            spaceClient.kind = SpaceChannel(spaceClient, space)

            val spaceChannel = spaceClient.kind as SpaceChannel
            session.spaces.add(spaceChannel)

            logger.info { "Init replay space channel" }
            spaceChannel.init()
            spaceChannel
          }

          val gameObject = space.objects.get(objectId) ?: error("Game object with ID $objectId not found in space ${space.id}")
          val replayEvent = ReplayEvent(timestamp, spaceChannel, gameObject, event)
          logger.info { "Replaying: $replayEvent" }
          emit(replayEvent)

          inBuffer.release()
        } catch(e: Exception) {
          logger.error(e) { "Error parsing replay line: $line" }
        }
      }
    }
  }
}

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
