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

package jp.assasans.narukami.server.core.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battleselect.BattleSelectModelCC
import jp.assasans.narukami.server.battleselect.BattleSelectTemplate
import jp.assasans.narukami.server.core.IEvent
import jp.assasans.narukami.server.core.IRegistry
import jp.assasans.narukami.server.core.ISpace
import jp.assasans.narukami.server.core.models
import jp.assasans.narukami.server.entrance.*
import jp.assasans.narukami.server.lobby.*
import jp.assasans.narukami.server.lobby.communication.*
import jp.assasans.narukami.server.lobby.user.RankInfo
import jp.assasans.narukami.server.lobby.user.RankLoaderModelCC
import jp.assasans.narukami.server.lobby.user.RankLoaderTemplate
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

class SpaceInitializer(
  private val spaces: IRegistry<ISpace>
) : IEvent, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  fun init() {
    spaces.add(Space(0xaa55).apply {
      logger.info { "EntranceClass models: ${EntranceTemplate::class.models}" }

      val entranceClass = TemplatedGameClass.fromTemplate(EntranceTemplate::class)
      val entranceObject = TransientGameObject.instantiate(
        id = 2,
        entranceClass,
        EntranceTemplate(
          entrance = EntranceModelCC(antiAddictionEnabled = false),
          captcha = CaptchaModelCC(stateWithCaptcha = listOf()),
          login = LoginModelCC(),
          registration = RegistrationModelCC(
            bgResource = gameResourceRepository.get("entrance.background", emptyMap(), ImageRes, Eager),
            enableRequiredEmail = false,
            minPasswordLength = 6,
            maxPasswordLength = 20
          ),
          entranceAlert = EntranceAlertModelCC()
        )
      )

      objects.add(entranceObject)
    })

    spaces.add(Space(0x55aa).apply {
      val lobbyClass = TemplatedGameClass.fromTemplate(LobbyTemplate::class)
      val lobbyObject = TransientGameObject.instantiate(
        id = 2,
        lobbyClass,
        LobbyTemplate(
          lobbyLayoutNotify = LobbyLayoutNotifyModelCC(),
          lobbyLayout = LobbyLayoutModelCC(),
          panel = PanelModelCC(),
          onceADayAction = OnceADayActionModelCC(todayRestartTime = 0),
        )
      )

      objects.add(lobbyObject)

      val rankLoaderObject = TransientGameObject.instantiate(
        id = 8,
        parent = TemplatedGameClass.fromTemplate(RankLoaderTemplate::class),
        RankLoaderTemplate(
          rankLoader = RankLoaderModelCC(
            ranks = listOf(
              RankInfo(index = 1, name = "Новобранец"),
              RankInfo(index = 2, name = "Рядовой"),
              RankInfo(index = 3, name = "Ефрейтор"),
              RankInfo(index = 4, name = "Капрал"),
              RankInfo(index = 5, name = "Мастер-капрал"),
              RankInfo(index = 6, name = "Сержант"),
              RankInfo(index = 7, name = "Штаб-сержант"),
              RankInfo(index = 8, name = "Мастер-сержант"),
              RankInfo(index = 9, name = "Первый сержант"),
              RankInfo(index = 10, name = "Сержант-майор"),
              RankInfo(index = 11, name = "Уорэнт-офицер 1"),
              RankInfo(index = 12, name = "Уорэнт-офицер 2"),
              RankInfo(index = 13, name = "Уорэнт-офицер 3"),
              RankInfo(index = 14, name = "Уорэнт-офицер 4"),
              RankInfo(index = 15, name = "Уорэнт-офицер 5"),
              RankInfo(index = 16, name = "Младший лейтенант"),
              RankInfo(index = 17, name = "Лейтенант"),
              RankInfo(index = 18, name = "Старший лейтенант"),
              RankInfo(index = 19, name = "Капитан"),
              RankInfo(index = 20, name = "Майор"),
              RankInfo(index = 21, name = "Подполковник"),
              RankInfo(index = 22, name = "Полковник"),
              RankInfo(index = 23, name = "Бригадир"),
              RankInfo(index = 24, name = "Генерал-майор"),
              RankInfo(index = 25, name = "Генерал-лейтенант"),
              RankInfo(index = 26, name = "Генерал"),
              RankInfo(index = 27, name = "Маршал"),
              RankInfo(index = 28, name = "Фельдмаршал"),
              RankInfo(index = 29, name = "Командор"),
              RankInfo(index = 30, name = "Генералиссимус"),
              RankInfo(index = 31, name = "Легенда")
            )
          )
        )
      )
      objects.add(rankLoaderObject)

      val communicationClass = TemplatedGameClass.fromTemplate(CommunicationTemplate::class)
      val communicationObject = TransientGameObject.instantiate(
        5,
        communicationClass,
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
      )
      objects.add(communicationObject)

      val battleSelectClass = TemplatedGameClass.fromTemplate(BattleSelectTemplate::class)
      val battleSelectObject = TransientGameObject.instantiate(
        6,
        battleSelectClass,
        BattleSelectTemplate(
          battleSelect = BattleSelectModelCC()
        )
      )
      objects.add(battleSelectObject)
    })
  }
}
