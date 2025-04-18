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

package jp.assasans.narukami.server.dispatcher

import jp.assasans.narukami.server.core.IEvent

/**
 * Loads contained in the event resources on the client before scheduling the actual event.
 *
 * Used when client event contains resources that need to be loaded beforehand,
 * e.g. in [jp.assasans.narukami.server.entrance.EntranceAlertModelShowAlertEvent].
 */
data class PreloadResourcesWrappedEvent<T : IEvent>(
  val inner: T
) : IEvent
