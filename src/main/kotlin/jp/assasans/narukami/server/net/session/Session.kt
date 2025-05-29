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

package jp.assasans.narukami.server.net.session

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IPending
import jp.assasans.narukami.server.core.IRegistry
import jp.assasans.narukami.server.core.ISessionRegistry
import jp.assasans.narukami.server.core.impl.Registry
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.net.ControlChannel
import jp.assasans.narukami.server.net.SpaceChannel

/**
 * Session represents a unique client instance connected to the server.
 * It is identified by a session hash, contains a control channel and zero or more space channels.
 *
 * @see jp.assasans.narukami.server.core.ArchitectureDocs
 */
interface ISession {
  val hash: SessionHash
  val properties: Map<String, String>
  val controlChannel: ControlChannel

  val spaces: IRegistry<SpaceChannel>
  val pendingSpaces: IRegistry<IPending<SpaceChannel>>

  var user: IGameObject?

  suspend fun close()
}

val ISession.userNotNull: IGameObject
  get() = checkNotNull(user) { "User is null for $this" }

class Session(
  override val hash: SessionHash,
  override val properties: Map<String, String>,
  override val controlChannel: ControlChannel
) : ISession, KoinComponent {
  private val logger = KotlinLogging.logger { }

  override val spaces: IRegistry<SpaceChannel> = Registry("Space channel") { space.id }
  override val pendingSpaces: IRegistry<IPending<SpaceChannel>> = Registry("Pending space channel") { id }

  override var user: IGameObject? = null

  private var open: Boolean = true

  private val sessions: ISessionRegistry by inject()

  override suspend fun close() {
    if(!open) return
    open = false

    logger.trace { "Closing $this" }

    val user = user
    if(user != null) {
      val lobbyChannel = checkNotNull(spaces.get(Space.stableId("lobby"))) {
        "Lobby channel not found for $this"
      }
      lobbyChannel.space.objects.remove(user)
      this.user = null
    }

    for(channel in spaces.all) {
      channel.close()
    }
    controlChannel.socket.close()

    sessions.remove(this)
    logger.info { "Closed $this" }
  }

  override fun toString(): String {
    return "Session(hash=$hash, controlChannel=$controlChannel)"
  }
}
