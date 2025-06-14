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

package jp.assasans.narukami.server.lobby.user

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolModel
import jp.assasans.narukami.server.protocol.ProtocolStruct

@ProtocolModel(1893408944113965505)
data class UserPropertiesModelCC(
  val canUseGroup: Boolean,
  val crystals: Int,
  val crystalsRating: Int,
  val daysFromLastVisit: Int,
  val daysFromRegistration: Int,
  val gearScore: Int,
  val goldsTakenRating: Int,
  val hasSpectatorPermissions: Boolean,
  val id: Long,
  val rank: Int,
  val rankBounds: RankBounds,
  val registrationTimestamp: Int,
  val score: Int,
  val scoreRating: Int,
  val uid: String,
  val userProfileUrl: String,
  val userRating: Int,
) : IModelConstructor

@ProtocolStruct
data class RankBounds(
  val lowBound: Int,
  val topBound: Int,
)
