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
import jp.assasans.narukami.server.battlefield.TankNodeV2
import jp.assasans.narukami.server.battlefield.tank.*
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.lobby.communication.remote

data class DamageIndicatorNode(
  val damageIndicator: DamageIndicatorModelCC,
) : Node()

@MatchTemplate(TankTemplate::class)
data class DamageTankNode(
  val health: HealthComponent,
) : NodeV2()

data class DamageEvent(
  val source: IGameObject,
  val amount: Float,
) : IEvent

class DamageSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun damage(
    context: IModelContext,
    event: DamageEvent,
    tank: DamageTankNode,
  ) = context {
    require(event.amount >= 0) { "Damage amount must be non-negative, got ${event.amount}" }

    val tankShared = remote(tank)

    tank.health.health = (tank.health.health - event.amount).coerceIn(0f..tank.health.maxHealth)
    TankModelSetHealthEvent(tank.health.health).schedule(tank, tankShared)

    if(tank.health.health <= 0) {
      TankModelKillEvent(event.source.id, 1000, DamageType.ISIS).schedule(tank, tankShared)
    }
  }

  @OnEventFireV2
  fun deathConfirmation(
    context: IModelContext,
    event: TankModelDeathConfirmationCommandEvent,
    tank: TankNodeV2,
  ) = context {
    val tankShared = remote(tank)
    TankModelDeathConfirmedEvent().schedule(tank, tankShared - context)
  }
}
