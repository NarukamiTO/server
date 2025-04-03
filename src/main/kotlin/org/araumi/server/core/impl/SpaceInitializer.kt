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

package org.araumi.server.core.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.core.IEvent
import org.araumi.server.core.IRegistry
import org.araumi.server.core.ISpace
import org.araumi.server.core.models
import org.araumi.server.entrance.*
import org.araumi.server.lobby.*
import org.araumi.server.res.Eager
import org.araumi.server.res.ImageRes
import org.araumi.server.res.RemoteGameResourceRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SpaceInitializer(
  private val spaces: IRegistry<ISpace>
) : IEvent, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  fun init() {
    spaces.add(Space(0xaa55).apply {
      logger.info { "EntranceClass models: ${EntranceTemplate::class.models}" }

      val entranceClass = TemplatedGameClass.fromTemplate(EntranceTemplate::class)
      val entranceObject = TransientGameObject.instantiate(
        id = 2,
        entranceClass,
        EntranceTemplate(
          entrance = EntranceModelCC(antiAddictionEnabled = false),
          captcha = CaptchaModelCC(stateWithCaptcha = listOf()),
          login = LoginModelCC(),
          registration = RegistrationModelCC(
            bgResource = gameResourceRepository.get("auth.registration.background-agpl", emptyMap(), ImageRes, Eager),
            enableRequiredEmail = false,
            minPasswordLength = 6,
            maxPasswordLength = 20
          ),
          entranceAlert = EntranceAlertModelCC()
        )
      )

      objects.add(entranceObject)
    })

    spaces.add(Space(0x55aa).apply {
      val lobbyClass = TemplatedGameClass.fromTemplate(LobbyTemplate::class)
      val lobbyObject = TransientGameObject.instantiate(
        id = 2,
        lobbyClass,
        LobbyTemplate(
          lobbyLayoutNotify = LobbyLayoutNotifyModelCC(),
          lobbyLayout = LobbyLayoutModelCC(),
          panel = PanelModelCC(),
          onceADayAction = OnceADayActionModelCC(todayRestartTime = 0),
        )
      )

      objects.add(lobbyObject)
    })
  }
}
