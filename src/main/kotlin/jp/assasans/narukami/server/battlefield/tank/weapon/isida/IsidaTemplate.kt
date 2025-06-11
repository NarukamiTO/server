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

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.LightEffectItem
import jp.assasans.narukami.server.battlefield.LightingSFXEntity
import jp.assasans.narukami.server.battlefield.tank.weapon.*
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.core.getComponent
import jp.assasans.narukami.server.extensions.toRadians
import jp.assasans.narukami.server.garage.item.ModificationComponent
import jp.assasans.narukami.server.res.*

object IsidaTemplate : WeaponTemplate(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  // Note: Isida is a stream weapon, but it does not use the StreamWeaponModelCC.
  override fun create(id: Long, marketItem: IGameObject) = super.create(id, marketItem).apply {
    val modification = marketItem.getComponent<ModificationComponent>().modification.toString()

    addModel(
      RotatingTurretModelCC(
        turretState = TurretStateCommand(
          controlInput = 0f,
          controlType = TurretControlType.ROTATION_DIRECTION,
          direction = 0f,
          rotationSpeedNumber = 100
        )
      )
    )
    addModel(
      WeaponVerticalAnglesModelCC(
        angleDown = 0.2f,
        angleUp = 0.2f
      )
    )
    addModel(
      IsisModelCC(
        capacity = 1000.0f,
        chargeRate = 100.0f,
        checkPeriodMsec = 250,
        coneAngle = 135.0f.toRadians(),
        dischargeDamageRate = 10.0f,
        dischargeHealingRate = 10.0f,
        dischargeIdleRate = 5.0f,
        radius = 20000.0f,
      )
    )
    // Modern Isida does not use all 1.0 resources. I'd like to port old Isida to the modern client as an extension.
    addModel(
      IsisSFXModelCC(
        damagingBall = gameResourceRepository.get("tank.weapon.isida.sfx.damage.start", mapOf("gen" to "1.0", "modification" to modification), MultiframeTextureRes, Eager),
        damagingRay = gameResourceRepository.get("tank.weapon.isida.sfx.damage.ray.static", mapOf("gen" to "1.0", "modification" to modification), TextureRes, Eager),
        damagingSound = gameResourceRepository.get("tank.weapon.isida.sfx.sound", mapOf("gen" to "1.0"), SoundRes, Eager),
        healingBall = gameResourceRepository.get("tank.weapon.isida.sfx.heal.start", mapOf("gen" to "1.0", "modification" to modification), MultiframeTextureRes, Eager),
        healingRay = gameResourceRepository.get("tank.weapon.isida.sfx.heal.ray.static", mapOf("gen" to "1.0", "modification" to modification), TextureRes, Eager),
        healingSound = gameResourceRepository.get("tank.weapon.isida.sfx.sound", mapOf("gen" to "1.0"), SoundRes, Eager),
        idleSound = gameResourceRepository.get("tank.weapon.isida.sfx.sound", mapOf("gen" to "1.0"), SoundRes, Eager),
        // XXX: Value from HTML5 client
        lightingSFXEntity = LightingSFXEntity(
          "enemyBeam" to listOf(
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0xFD4210", intensity = 0.7f, time = 0),
            LightEffectItem(attenuationBegin = 100f, attenuationEnd = 300f, color = "0xFD4210", intensity = 0.7f, time = 300),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0xFD4210", intensity = 0.7f, time = 600),
          ),
          "enemyLoop" to listOf(
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0xFD4210", intensity = 0.7f, time = 0),
            LightEffectItem(attenuationBegin = 100f, attenuationEnd = 300f, color = "0xFD4210", intensity = 0.7f, time = 300),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0xFD4210", intensity = 0.7f, time = 600),
          ),
          "enemyStart" to listOf(
            LightEffectItem(attenuationBegin = 1f, attenuationEnd = 2f, color = "0xFD4210", intensity = 0f, time = 0),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0xFD4210", intensity = 0.7f, time = 300),
          ),
          "friendBeam" to listOf(
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FFAA", intensity = 0.7f, time = 0),
            LightEffectItem(attenuationBegin = 100f, attenuationEnd = 300f, color = "0x00FFAA", intensity = 0.7f, time = 300),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FFAA", intensity = 0.7f, time = 600),
          ),
          "friendLoop" to listOf(
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FFAA", intensity = 0.7f, time = 0),
            LightEffectItem(attenuationBegin = 100f, attenuationEnd = 300f, color = "0x00FFAA", intensity = 0.7f, time = 300),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FFAA", intensity = 0.7f, time = 600),
          ),
          "friendStart" to listOf(
            LightEffectItem(attenuationBegin = 1f, attenuationEnd = 2f, color = "0x00FFAA", intensity = 0f, time = 0),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FFAA", intensity = 0.7f, time = 300),
          ),
          "loop" to listOf(
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FF55", intensity = 1f, time = 0),
            LightEffectItem(attenuationBegin = 100f, attenuationEnd = 300f, color = "0x00FF55", intensity = 1f, time = 300),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FF55", intensity = 1f, time = 600),
          ),
          "start" to listOf(
            LightEffectItem(attenuationBegin = 0f, attenuationEnd = 0f, color = "0x00FF55", intensity = 0f, time = 0),
            LightEffectItem(attenuationBegin = 200f, attenuationEnd = 400f, color = "0x00FF55", intensity = 1f, time = 300),
          ),
        )
      )
    )
  }
}
