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

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import jp.assasans.narukami.server.protocol.Codec
import jp.assasans.narukami.server.protocol.ICodec
import jp.assasans.narukami.server.protocol.IProtocol
import jp.assasans.narukami.server.protocol.ProtocolBuffer

/**
 * First client-to-server command, sent right after the control channel is opened.
 * This command is always sent unencrypted.
 *
 * Properties contains the information about the client, official client sends the following properties:
 *
 * Query parameters (non-exhaustive, all specified query parameters are sent):
 * - `balancer: http://127.0.0.1:8081/s/status.js`
 * - `debug: true`
 * - `locale: ru`
 * - `resources: 127.0.0.1:8082`
 * - `prefix: main.c`
 * - `config: 127.0.0.1:8081/config.xml`
 * - `lang: ru`
 *
 * Client information:
 * - `clientHashURL: #`
 * - `flash_player_type: StandAlone`
 * - `os: LNX` (optional)
 * - `flash_player_version: 32,0,0,465` (optional)
 * - `browser_user_agent: Mozilla/5.0 (X11, Linux x86_64, rv:137.0) Gecko/20100101 Firefox/137.0` (optional)
 */
data class HashRequestCommand(
  val properties: Map<String, String>
) : ControlCommand

/**
 * Properties are stored as two consecutive lists of keys and values.
 */
class HashRequestCommandCodec : Codec<HashRequestCommand>() {
  private lateinit var stringListCodec: ICodec<List<String>>

  override fun init(protocol: IProtocol) {
    super.init(protocol)
    stringListCodec = protocol.getCodec(List::class.createType(listOf(KTypeProjection.invariant(String::class.createType())))) as ICodec<List<String>>
  }

  override fun encode(buffer: ProtocolBuffer, value: HashRequestCommand) {
    stringListCodec.encode(buffer, value.properties.keys.toList())
    stringListCodec.encode(buffer, value.properties.values.toList())
  }

  override fun decode(buffer: ProtocolBuffer): HashRequestCommand {
    val keys = stringListCodec.decode(buffer)
    val values = stringListCodec.decode(buffer)

    val properties = keys.zip(values).toMap()
    return HashRequestCommand(properties)
  }
}
