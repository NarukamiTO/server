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

package jp.assasans.narukami.server.battlefield.tank.weapon.smoky

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battlefield.tank.TankGroupComponent
import jp.assasans.narukami.server.core.*

data class SmokyNode(
  val smokyModel: SmokyModelCC,
) : Node()

class SmokySystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  fun fire(
    event: SmokyModelFireCommandEvent,
    smoky: SmokyNode,
    @PerChannel smokyShared: List<SmokyNode>,
  ) {
    logger.trace { "Fire: $event" }

    SmokyModelShootEvent(
      shooterId = smoky.gameObject.getComponent<TankGroupComponent>().reference.id,
    ).schedule(smokyShared - smoky)
  }

  @OnEventFire
  @Mandatory
  fun fireStatic(
    event: SmokyModelFireStaticCommandEvent,
    smoky: SmokyNode,
    @PerChannel smokyShared: List<SmokyNode>,
  ) {
    logger.trace { "Fire static: $event" }

    SmokyModelShootStaticEvent(
      shooterId = smoky.gameObject.getComponent<TankGroupComponent>().reference.id,
      hitPoint = event.hitPoint,
    ).schedule(smokyShared - smoky)
  }

  @OnEventFire
  @Mandatory
  fun fireTarget(
    event: SmokyModelFireTargetCommandEvent,
    smoky: SmokyNode,
    @PerChannel smokyShared: List<SmokyNode>,
  ) {
    logger.trace { "Fire target: $event" }

    SmokyModelShootTargetEvent(
      shooterId = smoky.gameObject.getComponent<TankGroupComponent>().reference.id,
      targetId = event.targetId,
      hitPoint = event.hitPoint,
      weakeningCoeff = 1f,
      isCritical = true,
    ).schedule(smokyShared - smoky)

    SmokyModelLocalCriticalHitEvent(
      targetId = event.targetId,
    ).schedule(smoky)
  }
}
