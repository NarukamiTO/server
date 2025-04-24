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

import jp.assasans.narukami.server.core.ClosureModelProvider
import jp.assasans.narukami.server.core.IModelProvider
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.core.ITemplateProvider
import jp.assasans.narukami.server.core.impl.RegistrationBackgroundComponent
import jp.assasans.narukami.server.core.impl.RegistrationPasswordLimitsComponent
import jp.assasans.narukami.server.lobby.user.adaptSingle
import jp.assasans.narukami.server.net.command.ProtocolClass

@ProtocolClass(2)
data class EntranceTemplate(
  val entrance: EntranceModelCC,
  val captcha: CaptchaModelCC,
  val login: LoginModelCC,
  val registration: IModelProvider<RegistrationModelCC>,
  val entranceAlert: EntranceAlertModelCC,
) : ITemplate {
  companion object {
    val Provider = ITemplateProvider {
      EntranceTemplate(
        entrance = EntranceModelCC(antiAddictionEnabled = false),
        captcha = CaptchaModelCC(stateWithCaptcha = listOf()),
        login = LoginModelCC(),
        registration = ClosureModelProvider {
          val limits = it.adaptSingle<RegistrationPasswordLimitsComponent>()

          RegistrationModelCC(
            bgResource = it.adaptSingle<RegistrationBackgroundComponent>().resource,
            enableRequiredEmail = false,
            minPasswordLength = limits.minPasswordLength,
            maxPasswordLength = limits.maxPasswordLength,
          )
        },
        entranceAlert = EntranceAlertModelCC()
      )
    }
  }
}
