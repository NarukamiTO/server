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

package jp.assasans.narukami.server.battlefield.tank.weapon.isida

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.battlefield.tank.weapon.TargetHit
import jp.assasans.narukami.server.core.*

data class IsidaNode(
  val isida: IsisModelCC,
) : Node()

class IsidaSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  fun tick(
    event: IsisModelTickCommandEvent,
    isida: IsidaNode
  ) {
    logger.trace { "Tick: $event" }
  }

  @OnEventFire
  @Mandatory
  fun setTarget(
    event: IsisModelSetTargetCommandEvent,
    isida: IsidaNode,
    @PerChannel isidaShared: List<IsidaNode>,
  ) {
    logger.trace { "Set target: $event" }

    IsisModelSetTargetEvent(
      state = IsisState.DAMAGING,
      hit = TargetHit(
        direction = Vector3d(0f, 0f, 0f), // Unused
        localHitPoint = event.localHitPoint,
        numberHits = 0, // Unused
        target = event.target,
      ),
    ).schedule(isidaShared - isida)
  }

  @OnEventFire
  @Mandatory
  fun resetTarget(
    event: IsisModelResetTargetCommandEvent,
    isida: IsidaNode,
    @PerChannel isidaShared: List<IsidaNode>,
  ) {
    logger.trace { "Reset target: $event" }

    IsisModelResetTargetEvent().schedule(isidaShared - isida)
  }


  @OnEventFire
  @Mandatory
  fun stopWeapon(
    event: IsisModelStopWeaponCommandEvent,
    isida: IsidaNode,
    @PerChannel isidaShared: List<IsidaNode>,
  ) {
    logger.trace { "Stop weapon: $event" }

    IsisModelStopWeaponEvent().schedule(isidaShared - isida)
  }
}
