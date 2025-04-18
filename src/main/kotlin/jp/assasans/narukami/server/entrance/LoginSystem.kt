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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.dispatcher.preloadResources
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.LocalizedImageRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

data class EntranceNode(
  val entrance: EntranceModelCC,
) : Node()

class LoginSystem : AbstractSystem(), KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun login(
    event: LoginModelLoginEvent,
    entrance: EntranceNode,
    @JoinAll dispatcher: DispatcherNode,
  ) {
    logger.info { "Login event: $event" }

    if(event.password.isNotEmpty() && event.password.length % 2 == 0) {
      entrance.sender.sendBatched {
        LoginModelWrongPasswordEvent().attach(entrance).enqueue()
      }

      EntranceAlertModelShowAlertEvent(
        image = gameResourceRepository.get("alert.restrict", emptyMap(), LocalizedImageRes, Eager),
        header = "Login failed",
        text = "Wrong password"
      ).preloadResources().schedule(entrance)
      return
    }

    DispatcherOpenSpaceEvent(0x55aa).schedule(dispatcher).await()

    // Close the entrance space channel to trigger loading screen on the client
    entrance.sender.close()
  }
}
