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
import jp.assasans.narukami.server.battlefield.TankNodeV2
import jp.assasans.narukami.server.battlefield.tank.TankGroupComponent
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.lobby.communication.remote

@MatchTemplate(SmokyTemplate::class)
class SmokyNode : NodeV2()

class SmokySystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun fire(
    context: IModelContext,
    event: SmokyModelFireCommandEvent,
    smoky: SmokyNode,
    @JoinAll @JoinBy(TankGroupComponent::class) tank: TankNodeV2,
  ) = context {
    val smokyShared = remote(smoky)

    logger.trace { "Fire: $event" }

    SmokyModelShootEvent(
      shooterId = tank.gameObject.id,
    ).schedule(smoky, smokyShared - context)
  }

  @OnEventFireV2
  fun fireStatic(
    context: IModelContext,
    event: SmokyModelFireStaticCommandEvent,
    smoky: SmokyNode,
    @JoinAll @JoinBy(TankGroupComponent::class) tank: TankNodeV2,
    @PerChannel smokyShared: List<SmokyNode>,
  ) = context {
    val smokyShared = remote(smoky)

    logger.trace { "Fire static: $event" }

    SmokyModelShootStaticEvent(
      shooterId = tank.gameObject.id,
      hitPoint = event.hitPoint,
    ).schedule(smoky, smokyShared - context)
  }

  @OnEventFireV2
  fun fireTarget(
    context: IModelContext,
    event: SmokyModelFireTargetCommandEvent,
    smoky: SmokyNode,
    @JoinAll @JoinBy(TankGroupComponent::class) tank: TankNodeV2,
  ) = context {
    val smokyShared = remote(smoky)

    logger.trace { "Fire target: $event" }

    SmokyModelShootTargetEvent(
      shooterId = tank.gameObject.id,
      targetId = event.targetId,
      hitPoint = event.hitPoint,
      weakeningCoeff = 1f,
      isCritical = true,
    ).schedule(smoky, smokyShared - context)

    SmokyModelLocalCriticalHitEvent(
      targetId = event.targetId,
    ).schedule(smoky)
  }
}
