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

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.core.ITemplateProvider
import jp.assasans.narukami.server.net.command.ProtocolClass
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

@ProtocolClass(9)
data class MapInfoTemplate(
  val mapInfo: MapInfoModelCC,
) : ITemplate {
  companion object : KoinComponent {
    private val gameResourceRepository: RemoteGameResourceRepository by inject()

    val Provider = ITemplateProvider {
      MapInfoTemplate(
        mapInfo = MapInfoModelCC(
          defaultTheme = MapTheme.SUMMER_NIGHT,
          enabled = true,
          mapId = 7,
          mapName = "Spawn Test",
          matchmakingMark = false,
          maxPeople = 32,
          preview = gameResourceRepository.get(
            "map.spawn-test.preview",
            mapOf(
              "gen" to "2.1",
              "variant" to "default",
              "theme" to "summer",
              "time" to "day"
            ),
            ImageRes,
            Lazy
          ),
          rankLimit = Range(min = 1, max = 31),
          supportedModes = listOf(
            BattleMode.DM,
            BattleMode.TDM,
            BattleMode.CTF,
            BattleMode.CP,
            BattleMode.AS,
            BattleMode.RUGBY,
            BattleMode.SUR,
            BattleMode.JGR,
          ),
          theme = MapTheme.SUMMER_NIGHT
        )
      )
    }
  }
}
