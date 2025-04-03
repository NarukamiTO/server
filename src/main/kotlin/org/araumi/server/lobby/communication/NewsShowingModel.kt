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

package org.araumi.server.lobby.communication

import org.araumi.server.core.IModelConstructor
import org.araumi.server.net.command.ProtocolModel
import org.araumi.server.net.command.ProtocolStruct

@ProtocolModel(868364325972433765)
data class NewsShowingModelCC(
  val newsItems: List<NewsItemData>,
) : IModelConstructor

@ProtocolStruct
data class NewsItemData(
  /**
   * UTC UNIX timestamp in seconds.
   */
  // TODO: DateTime codec with @DateTimeRepresentation(UTC_SECONDS)
  val dateInSeconds: Int,
  val description: String,
  /**
   * UTC UNIX timestamp in seconds, `0` if not time-limited.
   */
  val endDate: Int,
  val header: String,
  val id: Long,
  val imageUrl: String,
)
