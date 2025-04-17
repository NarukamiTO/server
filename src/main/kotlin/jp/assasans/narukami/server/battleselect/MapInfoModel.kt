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

package jp.assasans.narukami.server.battleselect

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.Resource

@ProtocolModel(5412538083071671358)
data class MapInfoModelCC(
  val defaultTheme: MapTheme,
  val enabled: Boolean,
  val mapId: Long,
  val mapName: String,
  val matchmakingMark: Boolean,
  val maxPeople: Short,
  val preview: Resource<ImageRes, Lazy>,
  val rankLimit: Range,
  val supportedModes: List<BattleMode>,
  val theme: MapTheme,
) : IModelConstructor
