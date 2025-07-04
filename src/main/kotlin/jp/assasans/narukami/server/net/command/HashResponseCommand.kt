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

import jp.assasans.narukami.server.net.session.SessionHash
import jp.assasans.narukami.server.protocol.ProtocolPreserveOrder
import jp.assasans.narukami.server.protocol.ProtocolStruct

/**
 * Server-to-client command containing a session hash and encryption settings for the session.
 *
 * Official server sends it once as a response to the [HashRequestCommand].
 * This command is side effect free and can be sent multiple times, latest one will be used for new channels.
 */
@ProtocolStruct
@ProtocolPreserveOrder
data class HashResponseCommand(
  val hash: SessionHash,
  val channelProtectionEnabled: Boolean
) : ControlCommand
