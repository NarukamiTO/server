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

package jp.assasans.narukami.server.entrance

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.net.sessionNotNull

class EntranceSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @OutOfOrderExecution
  suspend fun channelAdded(
    event: ChannelAddedEvent,
    dispatcher: DispatcherNode,
    @JoinAll @AllowUnloaded entrance: EntranceNode
  ) {
    // Load the first ever game object. This will display a login screen.
    DispatcherLoadObjectsManagedEvent(entrance.gameObject).schedule(dispatcher).await()

    // One may pass "autologin=username:[password]" query parameter to the client,
    // skipping the login screen. Intended to be used for debugging purposes.
    val properties = event.channel.sessionNotNull.properties
    properties["autologin"]?.let {
      val username = it.substringBefore(':')
      val password = it.substringAfter(':')

      logger.info { "Performing autologin for $username" }
      LoginModelLoginEvent(
        uidOrEmail = username,
        password = password,
        remember = false,
      ).schedule(entrance)
    }
  }
}
