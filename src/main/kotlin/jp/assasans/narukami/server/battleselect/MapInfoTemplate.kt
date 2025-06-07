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

package jp.assasans.narukami.server.battleselect

import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.Resource

data class MapInfoComponent(
  val name: String,
  val theme: MapTheme,
  val defaultTheme: MapTheme,
  val preview: Resource<ImageRes, Lazy>,
) : IComponent

data class MapLimitsComponent(
  val maxPeople: Short,
  val rank: Range,
  val modes: List<BattleMode>,
) : IComponent

object MapInfoTemplate : PersistentTemplateV2() {
  override fun instantiate(id: Long) = gameObject(id).apply {
    addModel(ClosureModelProvider {
      val mapInfo = it.getComponent<MapInfoComponent>()
      val mapLimits = it.getComponent<MapLimitsComponent>()

      MapInfoModelCC(
        defaultTheme = mapInfo.defaultTheme,
        enabled = true,
        mapId = it.id,
        mapName = mapInfo.name,
        matchmakingMark = false,
        maxPeople = mapLimits.maxPeople,
        preview = mapInfo.preview,
        rankLimit = mapLimits.rank,
        supportedModes = mapLimits.modes,
        theme = mapInfo.theme,
      )
    })
  }
}
