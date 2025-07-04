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

package jp.assasans.narukami.server.battlefield.damage

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.battlefield.HealthComponent
import jp.assasans.narukami.server.battlefield.TankNode
import jp.assasans.narukami.server.battlefield.tank.*
import jp.assasans.narukami.server.core.*

data class DamageIndicatorNode(
  val damageIndicator: DamageIndicatorModelCC,
) : Node()

data class DamageTankNode(
  val tank: TankModelCC,
  val health: HealthComponent,
) : Node()

data class DamageEvent(
  val source: IGameObject,
  val amount: Float,
) : IEvent

class DamageSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  fun damage(
    event: DamageEvent,
    tank: DamageTankNode,
    @PerChannel tankShared: List<DamageTankNode>,
  ) {
    require(event.amount >= 0) { "Damage amount must be non-negative, got ${event.amount}" }

    tank.health.health = (tank.health.health - event.amount).coerceIn(0f..tank.health.maxHealth)
    TankModelSetHealthEvent(tank.health.health).schedule(tankShared)

    if(tank.health.health <= 0) {
      TankModelKillEvent(event.source.id, 1000, DamageType.ISIS).schedule(tankShared)
    }
  }

  @OnEventFire
  @Mandatory
  fun deathConfirmation(
    event: TankModelDeathConfirmationCommandEvent,
    tank: TankNode,
    @PerChannel tankShared: List<TankNode>,
  ) {
    TankModelDeathConfirmedEvent().schedule(tankShared - tank)
  }
}
