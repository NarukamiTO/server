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
import jp.assasans.narukami.server.battlefield.TankNodeV2
import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.battlefield.damage.DamageEvent
import jp.assasans.narukami.server.battlefield.tank.TankGroupComponent
import jp.assasans.narukami.server.battlefield.tank.weapon.TargetHit
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.lobby.communication.remote

@MatchTemplate(IsidaTemplate::class)
data class IsidaNode(
  val isidaTarget: IsidaTargetComponent?,
) : NodeV2()

data class IsidaTargetComponent(val target: IGameObject) : IComponent

class IsidaSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun tick(
    context: IModelContext,
    event: IsisModelTickCommandEvent,
    isida: IsidaNode,
    @JoinAll @JoinBy(TankGroupComponent::class) tank: TankNodeV2,
  ) = context {
    logger.trace { "Tick: $event" }

    if(isida.isidaTarget == null) {
      logger.warn { "Isida target is null, skipping tick" }
      return
    }

    DamageEvent(amount = 120f, source = tank.gameObject).schedule(context, isida.isidaTarget.target)
  }

  @OnEventFireV2
  fun setTarget(
    context: IModelContext,
    event: IsisModelSetTargetCommandEvent,
    isida: IsidaNode,
  ) = context {
    val isidaShared = remote(isida)

    logger.trace { "Set target: $event" }
    isida.gameObject.removeComponentIfPresent<IsidaTargetComponent>()
    isida.gameObject.addComponent(IsidaTargetComponent(event.target))

    IsisModelSetTargetEvent(
      state = IsisState.DAMAGING,
      hit = TargetHit(
        direction = Vector3d(0f, 0f, 0f), // Unused
        localHitPoint = event.localHitPoint,
        numberHits = 0, // Unused
        target = event.target,
      ),
    ).schedule(isida, isidaShared - context)
  }

  @OnEventFireV2
  fun resetTarget(
    context: IModelContext,
    event: IsisModelResetTargetCommandEvent,
    isida: IsidaNode,
  ) = context {
    val isidaShared = remote(isida)

    logger.trace { "Reset target: $event" }
    isida.gameObject.removeComponentIfPresent<IsidaTargetComponent>()

    IsisModelResetTargetEvent().schedule(isida, isidaShared - context)
  }

  @OnEventFireV2
  fun stopWeapon(
    context: IModelContext,
    event: IsisModelStopWeaponCommandEvent,
    isida: IsidaNode,
  ) = context {
    val isidaShared = remote(isida)

    logger.trace { "Stop weapon: $event" }

    IsisModelStopWeaponEvent().schedule(isida, isidaShared - context)
  }
}
