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

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*

class BattleCreateSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun checkName(
    context: IModelContext,
    event: BattleCreateModelCheckBattleNameForForbiddenWordsEvent,
    battleCreate: BattleCreateNodeV2,
  ) = context {
    val filteredName = event.battleName.replace("2112", "2019")
    BattleCreateModelSetFilteredBattleNameEvent(filteredName).schedule(battleCreate)
  }

  @OnEventFireV2
  fun create(
    context: IModelContext,
    event: BattleCreateModelCreateBattleEvent,
    battleCreate: BattleCreateNodeV2,
  ) = context {
    logger.warn { "Battle creation is not implemented" }
    BattleCreateModelCreateFailedYouAreBannedEvent().schedule(battleCreate)
  }
}
