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

package jp.assasans.narukami.server.lobby

import jp.assasans.narukami.server.core.ClosureModelProvider
import jp.assasans.narukami.server.core.PersistentTemplateV2
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.core.requireSpaceChannel
import jp.assasans.narukami.server.net.sessionNotNull

object LobbyTemplate : PersistentTemplateV2() {
  override fun instantiate(id: Long) = gameObject(id).apply {
    addModel(LobbyLayoutNotifyModelCC())
    addModel(LobbyLayoutModelCC())
    addModel(PanelModelCC())
    addModel(OnceADayActionModelCC(todayRestartTime = 0))
    addModel(ClosureModelProvider {
      val configPublicUrl = System.getProperty("config.url") ?: error("\"config.url\" system property is not set")
      ReconnectModelCC(
        configUrlTemplate = requireSpaceChannel.sessionNotNull.properties["config"] ?: "$configPublicUrl/config.xml",
        serverNumber = 1,
      )
    })
    addModel(GPUDetectorModelCC())
  }
}
