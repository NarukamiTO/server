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

package jp.assasans.narukami.server.battlefield.tank.weapon.railgun

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battlefield.TankNodeV2
import jp.assasans.narukami.server.battlefield.tank.TankGroupComponent
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.lobby.communication.remote

@MatchTemplate(RailgunTemplate::class)
class RailgunNode : NodeV2()

class RailgunSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun startCharging(
    context: IModelContext,
    event: RailgunModelStartChargingCommandEvent,
    railgun: RailgunNode,
    @JoinAll @JoinBy(TankGroupComponent::class) tank: TankNodeV2,
  ) = context {
    val railgunShared = remote(railgun)

    logger.trace { "Start charging: $event" }

    RailgunModelStartChargingEvent(
      shooter = tank.gameObject,
    ).schedule(railgun, railgunShared - context)
  }

  @OnEventFireV2
  fun fireDummy(
    context: IModelContext,
    event: RailgunModelFireCommandEvent,
    railgun: RailgunNode,
    @JoinAll @JoinBy(TankGroupComponent::class) tank: TankNodeV2,
  ) = context {
    val railgunShared = remote(railgun)

    logger.trace { "Fire dummy: $event" }

    RailgunModelFireEvent(
      shooter = tank.gameObject,
      staticHitPoint = event.staticHitPoint,
      targets = event.targets,
      targetHitPoints = event.targetHitPoints,
    ).schedule(railgun, railgunShared - context)
  }
}
