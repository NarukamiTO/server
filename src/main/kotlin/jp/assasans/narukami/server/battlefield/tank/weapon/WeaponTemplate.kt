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

package jp.assasans.narukami.server.battlefield.tank.weapon

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.tank.hull.MarketItemGroupComponent
import jp.assasans.narukami.server.battlefield.tank.hull.asModel
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.TemplateV2
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.core.getComponent
import jp.assasans.narukami.server.extensions.toRadians
import jp.assasans.narukami.server.garage.item.*
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.RemoteGameResourceRepository
import jp.assasans.narukami.server.res.SoundRes

abstract class WeaponTemplate : TemplateV2(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  open fun create(id: Long, marketItem: IGameObject) = gameObject(id).apply {
    addComponent(MarketItemGroupComponent(marketItem))

    val properties = marketItem.getComponent<GaragePropertiesContainerComponent>()
    addModel(
      WeaponCommonModelCC(
        buffShotCooldownMs = 0,
        buffed = false,
        highlightingDistance = 1000f,
        impactForce = properties.getComponent<ImpactForceComponent>().impactForce,
        kickback = properties.getComponent<KickbackComponent>().kickback,
        turretRotationAcceleration = 1f,
        turretRotationSound = gameResourceRepository.get("tank.sound.weapon-rotate", mapOf(), SoundRes, Eager),
        turretRotationSpeed = properties.getComponent<TurretRotationSpeedComponent>().turretRotationSpeed.toRadians(),
      )
    )
    addModel(VerticalAutoAimingModelCC())
    addModel(marketItem.getComponent<Object3DComponent>().resource.asModel())
  }
}
