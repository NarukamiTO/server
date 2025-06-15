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
import java.nio.file.StandardOpenOption
import kotlin.reflect.full.createType
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBufAllocator
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.entrance.ExternGameObject
import jp.assasans.narukami.server.extensions.toHexString
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.session.SessionHash
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.protocol.ICodec
import jp.assasans.narukami.server.protocol.Protocol
import jp.assasans.narukami.server.protocol.ProtocolBuffer
import jp.assasans.narukami.server.protocol.ProtocolBufferCodec

class ReplayWriter(val space: ISpace) : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val objectMapper: ObjectMapper by inject()

  val writer = Files.newOutputStream(Paths.get("battle.replay"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)!!.writer()

  val protocol = Protocol(FakeSocket(space))
  val protocolBufferCodec = ProtocolBufferCodec()

  fun writeComment(comment: String) {
    writer.write("# $comment\n")
    writer.flush()
  }

  fun writeExternObject(gameObject: IGameObject) {
    val json = objectMapper.copy()
      .setDefaultPrettyPrinter(MinimalPrettyPrinter())
      .writeValueAsString(ExternGameObject(gameObject.id, gameObject.template as PersistentTemplateV2, gameObject.allComponents))

    val columns = listOf(
      "extern",
      Clock.System.now().toEpochMilliseconds(),
      gameObject.id,
      json,
    )
    write(columns)
  }

  fun writeUserObject(sessionHash: SessionHash, gameObject: IGameObject) {
    val json = objectMapper.copy()
      .setDefaultPrettyPrinter(MinimalPrettyPrinter())
      .writeValueAsString(ExternGameObject(gameObject.id, gameObject.template as PersistentTemplateV2, gameObject.allComponents))

    val columns = listOf(
      "user",
      Clock.System.now().toEpochMilliseconds(),
      sessionHash.value.toHexString(),
      json,
    )
    write(columns)
  }

  fun writeEvent(event: IEvent, gameObject: IGameObject, sender: SpaceChannel) {
    val codec = protocol.getCodec(event::class.createType()) as ICodec<IEvent>
    val buffer = ProtocolBuffer.Companion.default()
    codec.encode(buffer, event)
    val outBuffer = ByteBufAllocator.DEFAULT.buffer()
    protocolBufferCodec.encode(outBuffer, buffer)

    val columns = listOf(
      "event",
      Clock.System.now().toEpochMilliseconds(),
      sender.sessionNotNull.hash.value.toHexString(),
      gameObject.id,
      event::class.protocolId,
      outBuffer.toHexString(),
    )
    write(columns)
  }

  fun write(columns: List<Any>) {
    writer.write(columns.joinToString(" ") + "\n")
    writer.flush()
  }
}
