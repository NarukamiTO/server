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

import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.core.ITemplateProvider
import jp.assasans.narukami.server.net.command.ProtocolClass

@ProtocolClass(6)
data class RankLoaderTemplate(
  val rankLoader: RankLoaderModelCC,
) : ITemplate {
  companion object {
    val Provider = ITemplateProvider {
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
    }
  }
}
