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
import jp.assasans.narukami.server.battlefield.tank.TankGroupComponent
import jp.assasans.narukami.server.core.*

data class RailgunNode(
  val railgun: RailgunModelCC,
) : Node()

class RailgunSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  fun startCharging(
    event: RailgunModelStartChargingCommandEvent,
    railgun: RailgunNode,
    @PerChannel railgunShared: List<RailgunNode>,
  ) {
    logger.trace { "Start charging: $event" }

    RailgunModelStartChargingEvent(
      shooter = railgun.gameObject.getComponent<TankGroupComponent>().reference,
    ).schedule(railgunShared - railgun)
  }

  @OnEventFire
  @Mandatory
  fun fireDummy(
    event: RailgunModelFireCommandEvent,
    railgun: RailgunNode,
    @PerChannel railgunShared: List<RailgunNode>,
  ) {
    logger.trace { "Fire dummy: $event" }

    RailgunModelFireEvent(
      shooter = railgun.gameObject.getComponent<TankGroupComponent>().reference,
      staticHitPoint = event.staticHitPoint,
      targets = event.targets,
      targetHitPoints = event.targetHitPoints,
    ).schedule(railgunShared - railgun)
  }
}
