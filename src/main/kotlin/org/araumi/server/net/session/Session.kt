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

package org.araumi.server.net.session

import org.araumi.server.core.IPending
import org.araumi.server.core.IRegistry
import org.araumi.server.core.impl.Registry
import org.araumi.server.net.ControlChannel
import org.araumi.server.net.SpaceChannel

/**
 * Session represents a unique client instance connected to the server.
 * It is identified by a session hash and contains multiple channel sockets.
 */
interface ISession {
  val hash: SessionHash
  val controlChannel: ControlChannel

  val spaces: IRegistry<SpaceChannel>
  val pendingSpaces: IRegistry<IPending<SpaceChannel>>
}

class Session(
  override val hash: SessionHash,
  override val controlChannel: ControlChannel
) : ISession {
  override val spaces: IRegistry<SpaceChannel> = Registry("Space channel") { space.id }
  override val pendingSpaces: IRegistry<IPending<SpaceChannel>> = Registry("Pending space channel") { id }

  override fun toString(): String {
    return "Session(hash=$hash, controlChannel=$controlChannel)"
  }
}
