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

import org.araumi.server.core.IClientEvent
import org.araumi.server.core.IModelConstructor
import org.araumi.server.net.command.ProtocolEvent
import org.araumi.server.net.command.ProtocolModel
import org.araumi.server.res.Eager
import org.araumi.server.res.LocalizedImageRes
import org.araumi.server.res.Resource

@ProtocolModel(7840560143954508415)
class EntranceAlertModelCC : IModelConstructor

@ProtocolEvent(7216954482225034551)
class EntranceAlertModelShowAlertEvent(
  val image: Resource<LocalizedImageRes, Eager>,
  val header: String,
  val text: String
) : IClientEvent
