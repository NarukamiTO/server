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

import kotlin.time.Duration.Companion.minutes
import jp.assasans.narukami.server.core.PersistentTemplateV2
import jp.assasans.narukami.server.core.addModel

object BattleCreateTemplate : PersistentTemplateV2() {
  override fun instantiate(id: Long) = gameObject(id).apply {
    addModel(
      BattleCreateModelCC(
        battleCreationDisabled = false,
        battlesLimits = BattleMode.entries.map {
          BattleLimits(scoreLimit = 999, timeLimitInSec = 999.minutes.inWholeSeconds.toInt())
        },
        defaultRange = Range(min = 1, max = 31),
        maxRange = Range(min = 1, max = 31),
        maxRangeLength = 31,
        ultimatesEnabled = false
      )
    )
  }
}
