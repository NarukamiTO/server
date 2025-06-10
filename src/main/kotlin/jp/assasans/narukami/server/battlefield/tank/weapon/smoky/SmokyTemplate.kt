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
import jp.assasans.narukami.server.battlefield.tank.weapon.*
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.core.getComponent
import jp.assasans.narukami.server.garage.item.ModificationComponent
import jp.assasans.narukami.server.res.*

object SmokyTemplate : WeaponTemplate(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  override fun create(id: Long, marketItem: IGameObject) = super.create(id, marketItem).apply {
    // TODO: Should too precise namespaces be ignored? That way we could have one namespaces map for all resources.
    val modification = marketItem.getComponent<ModificationComponent>().modification.toString()

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
      criticalHitSize = 300, // Estimated value, reference: https://www.youtube.com/watch?v=U6OntcEwsX0
      criticalHitTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.critical", mapOf("gen" to "1.0"), MultiframeTextureRes, Eager),
      explosionMarkTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.hit-mark", mapOf("gen" to "1.0"), TextureRes, Eager),
      explosionSize = 600, // Value from GTanks client
      explosionSound = gameResourceRepository.get("tank.weapon.smoky.sfx.sound.hit", mapOf("gen" to "1.0"), SoundRes, Eager),
      explosionTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.explosion", mapOf("gen" to "1.0", "modification" to modification), MultiframeTextureRes, Eager),
      // XXX: Value from HTML5 client
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
        )
      ),
      shotSound = gameResourceRepository.get("tank.weapon.smoky.sfx.sound.shot", mapOf("gen" to "1.0"), SoundRes, Eager),
      shotTexture = gameResourceRepository.get("tank.weapon.smoky.sfx.shot", mapOf("gen" to "1.0", "modification" to modification), TextureRes, Eager)
    ))
  }
}
