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

import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.net.command.ProtocolClass
import jp.assasans.narukami.server.net.sessionNotNull

@ProtocolClass(3)
data class LobbyTemplate(
  val lobbyLayoutNotify: LobbyLayoutNotifyModelCC,
  val lobbyLayout: LobbyLayoutModelCC,
  val panel: PanelModelCC,
  val onceADayAction: OnceADayActionModelCC,
  val reconnect: IModelProvider<ReconnectModelCC>,
  val gpuDetector: GPUDetectorModelCC,
) : ITemplate {
  companion object {
    val Provider = ITemplateProvider {
      LobbyTemplate(
        lobbyLayoutNotify = LobbyLayoutNotifyModelCC(),
        lobbyLayout = LobbyLayoutModelCC(),
        panel = PanelModelCC(),
        onceADayAction = OnceADayActionModelCC(todayRestartTime = 0),
        reconnect = ClosureModelProvider {
          val configPublicUrl = System.getProperty("config.url") ?: error("\"config.url\" system property is not set")
          ReconnectModelCC(
            configUrlTemplate = requireSpaceChannel.sessionNotNull.properties["config"] ?: "$configPublicUrl/config.xml",
            serverNumber = 1,
          )
        },
        gpuDetector = GPUDetectorModelCC(),
      )
    }
  }
}
