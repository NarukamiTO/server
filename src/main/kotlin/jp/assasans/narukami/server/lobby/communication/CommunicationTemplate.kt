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

package jp.assasans.narukami.server.lobby.communication

import kotlinx.datetime.Clock
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.core.ITemplateProvider
import jp.assasans.narukami.server.net.command.ProtocolClass

@ProtocolClass(5)
data class CommunicationTemplate(
  val communicationPanel: CommunicationPanelModelCC,
  val newsShowing: NewsShowingModelCC,
  val chat: ChatModelCC,
) : ITemplate {
  companion object {
    val Provider = ITemplateProvider {
      CommunicationTemplate(
        communicationPanel = CommunicationPanelModelCC(),
        newsShowing = NewsShowingModelCC(
          newsItems = listOf(
            NewsItemData(
              dateInSeconds = Clock.System.now().epochSeconds.toInt(),
              description = """
                Все всё равно знают, что Пэдди Мориарти, который жил в Австралии в деревне из 11 человек,
                пропал вместе со своей собакой. Все знают, что Пэдди часто ссорился с бабкой. Она, кстати,
                пекла пирожки из крокодила и могла сделать из Пэдди Мориарти пирог. Также большинство догадываются,
                что Пэдди умер из-за пива, ведь он пил его В ПРОЗРАЧНОМ СТАКАНЕ СО ШРЕКОМ!!!
              """.trimIndent(),
              endDate = 0,
              header = "Не нужно это скрывать!",
              id = 1,
              imageUrl = "https://files.catbox.moe/99m151.png"
            )
          )
        ),
        chat = ChatModelCC(
          admin = true,
          antifloodEnabled = false,
          bufferSize = 100,
          channels = listOf(
            "General",
            "Logs",
          ),
          chatEnabled = true,
          chatModeratorLevel = ChatModeratorLevel.ADMINISTRATOR,
          linksWhiteList = listOf("github.com"),
          minChar = 0,
          minWord = 0,
          privateMessagesEnabled = false,
          selfName = "",
          showLinks = true,
          typingSpeedAntifloodEnabled = false
        ),
      )
    }
  }
}
