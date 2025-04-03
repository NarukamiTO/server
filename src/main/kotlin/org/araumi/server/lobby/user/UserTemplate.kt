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

package org.araumi.server.lobby.user

import org.araumi.server.core.IModelProvider
import org.araumi.server.core.ITemplate
import org.araumi.server.net.command.ProtocolClass

@ProtocolClass(4)
data class UserTemplate(
  val rankLoader: RankLoaderModelCC,
  val userProperties: IModelProvider<UserPropertiesModelCC>,
  val userNotifier: UserNotifierModelCC,
  val uidNotifier: UidNotifierModelCC,
  val rankNotifier: RankNotifierModelCC,
) : ITemplate
