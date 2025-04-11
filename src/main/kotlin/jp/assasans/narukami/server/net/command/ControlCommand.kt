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

package jp.assasans.narukami.server.net.command

import kotlin.reflect.full.createType
import jp.assasans.narukami.server.protocol.Codec
import jp.assasans.narukami.server.protocol.ICodec
import jp.assasans.narukami.server.protocol.ProtocolBuffer
import jp.assasans.narukami.server.protocol.getTypedCodec

sealed interface ControlCommand

class ControlCommandCodec : Codec<ControlCommand>() {
  override fun encode(buffer: ProtocolBuffer, value: ControlCommand) {
    val command = when(value) {
      is HashRequestCommand  -> 1
      is HashResponseCommand -> 2
      is OpenSpaceCommand    -> 32
      is InitSpaceCommand    -> 3
    }
    buffer.data.writeByte(command)

    val codec = protocol.getCodec(value::class.createType()) as ICodec<ControlCommand>
    codec.encode(buffer, value)
  }

  override fun decode(buffer: ProtocolBuffer): ControlCommand {
    val command = buffer.data.readByte().toInt()
    return when(command) {
      1    -> protocol.getTypedCodec<HashRequestCommand>().decode(buffer)
      2    -> protocol.getTypedCodec<HashResponseCommand>().decode(buffer)
      32   -> protocol.getTypedCodec<OpenSpaceCommand>().decode(buffer)
      3    -> protocol.getTypedCodec<InitSpaceCommand>().decode(buffer)
      else -> throw IllegalArgumentException("Unknown command: $command")
    }
  }
}
