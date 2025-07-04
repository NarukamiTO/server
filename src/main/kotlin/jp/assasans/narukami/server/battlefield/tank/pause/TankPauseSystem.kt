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

package jp.assasans.narukami.server.battlefield.tank.pause

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battlefield.TankNodeV2
import jp.assasans.narukami.server.core.AbstractSystem
import jp.assasans.narukami.server.core.IModelContext
import jp.assasans.narukami.server.core.OnEventFireV2

class TankPauseSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun disablePause(
    context: IModelContext,
    event: TankPauseModelDisablePauseEvent,
    tank: TankNodeV2,
  ) {
    // TODO: Reset idle kick cooldown when implemented
  }
}
