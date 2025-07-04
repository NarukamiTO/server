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

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.full.createType
import com.fasterxml.jackson.databind.ObjectMapper
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
import jp.assasans.narukami.server.core.IEvent
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.ISessionRegistry
import jp.assasans.narukami.server.core.ISpace
import jp.assasans.narukami.server.net.ControlChannel
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.SpaceEventProcessor
import jp.assasans.narukami.server.net.session.Session
import jp.assasans.narukami.server.net.session.SessionHash
import jp.assasans.narukami.server.protocol.ICodec
import jp.assasans.narukami.server.protocol.Protocol
import jp.assasans.narukami.server.protocol.ProtocolBufferCodec

class ReplayReader(val space: ISpace) : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val objectMapper: ObjectMapper by inject()
  private val sessions: ISessionRegistry by inject()
  private val spaceEventProcessor: SpaceEventProcessor by inject()

  private val reader = Files.newBufferedReader(Paths.get("battle-2.replay"))

  val sessionToUser = mutableMapOf<SessionHash, IGameObject>()

  val protocol = Protocol(FakeSocket(space))
  val protocolBufferCodec = ProtocolBufferCodec()

  fun readEvents(): Flow<IReplayEntry> = flow {
    var firstTimestamp: Long? = null
    var startTime: Long? = null

    reader.useLines { lines ->
      for(line in lines) {
        try {
          if(line.isBlank()) continue
          if(line.startsWith("#")) continue
          logger.trace { "Reading replay line: $line" }

          val type = line.substringBefore(" ")
          val content = line.substringAfter(" ")
          when(type) {
            "user"   -> {
              val columns = content.split(" ")
              if(columns.size != 3) {
                logger.warn { "Invalid user line format: $line" }
                continue
              }

              val timestamp = columns[0].toLong()
              val sessionHash = SessionHash(columns[1].hexToByteArray())
              val gameObject = deserializeExternObject(objectMapper, columns[2], space.objects, isolate = true)

              logger.info { "Read user for $sessionHash: $gameObject" }
              sessionToUser[sessionHash] = gameObject
              emit(ReplayUser(timestamp, sessionHash, gameObject))
            }

            "extern" -> {
              val columns = content.split(" ")
              if(columns.size != 3) {
                logger.warn { "Invalid extern line format: $line" }
                continue
              }

              val timestamp = columns[0].toLong()
              val objectId = columns[1].toLong()
              val gameObject = deserializeExternObject(objectMapper, columns[2], space.objects, isolate = false)
              require(gameObject.id == objectId) { "Object ID mismatch: expected $objectId, got ${gameObject.id}" }

              logger.info { "Replaying extern object: $gameObject" }
              emit(ReplayExternObject(timestamp, gameObject))
            }

            "event"  -> {
              val columns = content.split(" ")
              if(columns.size != 5) {
                logger.warn { "Invalid replay line format: $content" }
                continue
              }

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

              val eventClass = requireNotNull(spaceEventProcessor.getClass(methodId)) { "Event class for method ID $methodId not found" }
              val codec = protocol.getCodec(eventClass.createType()) as ICodec<IEvent>
              val event = codec.decode(buffer)

              val session = if(sessions.has(sessionHash)) {
                sessions.get(sessionHash)!!
              } else {
                val controlClient = ReplaySocketClient(null, CoroutineScope(Dispatchers.IO))
                val session = Session(sessionHash, emptyMap(), controlClient.kind as ControlChannel)
                controlClient.session = session
                session.user = checkNotNull(sessionToUser[sessionHash]) { "User object for session $sessionHash not found" }
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

              val realObjectId = if(ISOLATED_MAPPING.containsKey(objectId)) {
                ISOLATED_MAPPING[objectId]!!
              } else {
                objectId
              }

              val gameObject = space.objects.get(realObjectId) ?: error("Game object with ID $objectId not found in space ${space.id}")
              val replayEvent = ReplayEvent(timestamp, spaceChannel, gameObject, event)
              logger.info { "Replaying: $replayEvent" }
              emit(replayEvent)

              inBuffer.release()
            }

            else     -> {
              logger.error { "Unknown replay line type: $type" }
            }
          }
        } catch(e: Exception) {
          logger.error(e) { "Error parsing replay line: $line" }
        }
      }
    }
  }
}

val ISOLATED_MAPPING = mutableMapOf<Long, Long>()

fun isolateId(id: Long): Long {
  val isolated = if(id < 0) {
    id and (1L shl 62).inv()
  } else {
    id or (1L shl 62)
  }
  check(isolated != id) { "Isolated ID must differ from original ID: $id" }

  ISOLATED_MAPPING[id] = isolated
  return isolated
}
