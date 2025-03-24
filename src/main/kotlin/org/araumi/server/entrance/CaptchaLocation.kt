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

package org.araumi.server.entrance

import org.araumi.server.net.command.IProtocolEnum
import org.araumi.server.net.command.ProtocolEnum

@ProtocolEnum
enum class CaptchaLocation(override val value: Int) : IProtocolEnum<Int> {
  LoginForm(0),
  RegisterForm(1),
  ClientStartup(2),
  RestorePasswordForm(3),
  EmailChangeHash(4),
  AccountSettingsHash(5),
}
