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

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.LightEffectItem
import jp.assasans.narukami.server.battlefield.LightingEffectEntity
import jp.assasans.narukami.server.battlefield.LightingSFXEntity
import jp.assasans.narukami.server.battlefield.tank.hull.asModel
import jp.assasans.narukami.server.battlefield.tank.weapon.*
import jp.assasans.narukami.server.core.PersistentTemplateV2
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.res.*

object SmokyTemplate : PersistentTemplateV2(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  override fun instantiate(id: Long) = gameObject(id).apply {
    addModel(WeaponCommonModelCC(
      buffShotCooldownMs = 0,
      buffed = false,
      highlightingDistance = 1000f,
      impactForce = 500f,
      kickback = 500f,
      turretRotationAcceleration = 1f,
      turretRotationSound = gameResourceRepository.get("tank.sound.weapon-rotate", mapOf(), SoundRes, Eager),
      turretRotationSpeed = 1f
    ))
    addModel(gameResourceRepository.get("tank.weapon.smoky", mapOf("gen" to "1.0", "modification" to "0"), Object3DRes, Eager).asModel())
    addModel(VerticalAutoAimingModelCC())
    addModel(RotatingTurretModelCC(
      turretState = TurretStateCommand(
        controlInput = 0f,
        controlType = TurretControlType.ROTATION_DIRECTION,
        direction = 0f,
        rotationSpeedNumber = 100
      )
    ))
    addModel(DiscreteShotModelCC(reloadMsec = 500))
    addModel(WeaponWeakeningModelCC(
      maximumDamageRadius = 500.0f,
      minimumDamagePercent = 10.0f,
      minimumDamageRadius = 1000.0f
    ))
    addModel(WeaponVerticalAnglesModelCC(
      angleDown = 0.2f,
      angleUp = 0.2f
    ))
    addModel(SplashModel(
      impactForce = 100f,
      minSplashDamagePercent = 5f,
      radiusOfMaxSplashDamage = 100f,
      splashDamageRadius = 1000f,
    ))
    addModel(SmokyModelCC())
    addModel(SmokyShootSFXModelCC(
      criticalHitSize = 1000,
      criticalHitTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.critical", mapOf(), MultiframeTextureRes, Eager),
      explosionMarkTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.hit-mark", mapOf(), TextureRes, Eager),
      explosionSize = 375,
      explosionSound = gameResourceRepository.get("tank.weapon.smoky.sfx.sound.hit", mapOf(), SoundRes, Eager),
      explosionTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.NC_explosion", mapOf(), MultiframeTextureRes, Eager),
      lightingSFXEntity = LightingSFXEntity(
        listOf(
          LightingEffectEntity(
            "hit", listOf(
              LightEffectItem(attenuationBegin = 170f, attenuationEnd = 300f, color = "0xffbf00", intensity = 1.7f, time = 0),
              LightEffectItem(attenuationBegin = 100f, attenuationEnd = 300f, color = "0xffbf00", intensity = 0f, time = 400)
            )
          ),
          LightingEffectEntity(
            "shot", listOf(
              LightEffectItem(attenuationBegin = 190f, attenuationEnd = 450f, color = "0xfcdd76", intensity = 1.9f, time = 0),
              LightEffectItem(attenuationBegin = 1f, attenuationEnd = 2f, color = "0xfcdd76", intensity = 0f, time = 300)
            )
          ),
          LightingEffectEntity(
            "shell", listOf(
              LightEffectItem(attenuationBegin = 0f, attenuationEnd = 0f, color = "0xfcdd76", intensity = 0f, time = 0)
            )
          )
        )
      ),
      shotSound = gameResourceRepository.get("tank.weapon.smoky.sfx.sound.shot", mapOf(), SoundRes, Eager),
      shotTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.shot", mapOf(), TextureRes, Eager)
    ))
  }
}
