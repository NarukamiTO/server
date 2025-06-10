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

package jp.assasans.narukami.server.battlefield.tank.hull

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.LightEffectItem
import jp.assasans.narukami.server.battlefield.LightingEffectEntity
import jp.assasans.narukami.server.battlefield.LightingSFXEntity
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.TemplateV2
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.core.getComponent
import jp.assasans.narukami.server.garage.item.*
import jp.assasans.narukami.server.res.*

object HullTemplate : TemplateV2(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  fun create(id: Long, marketItem: IGameObject) = gameObject(id).apply {
    addComponent(MarketItemGroupComponent(marketItem))

    val properties = marketItem.getComponent<GaragePropertiesContainerComponent>()
    addModel(HullCommonModelCC(
      deadColoring = gameResourceRepository.get("tank.dead", mapOf(), TextureRes, Eager),
      deathSound = gameResourceRepository.get("tank.sound.destroy", mapOf(), SoundRes, Eager),
      lightingSFXEntity = LightingSFXEntity(
        effects = listOf(
          LightingEffectEntity(
            "explosion", listOf(
              LightEffectItem(1f, 2f, "0xCCA538", 0f, 0),
              LightEffectItem(500f, 1500f, "0xCCA538", 1.2f, 100),
              LightEffectItem(1f, 2f, "0xCCA538", 0f, 1200)
            )
          )
        )
      ),
      mass = properties.getComponent<MassComponent>().mass,
      stunEffectTexture = gameResourceRepository.get("tank.stun.texture", mapOf(), TextureRes, Eager),
      stunSound = gameResourceRepository.get("tank.stun.sound", mapOf(), SoundRes, Eager),
      ultimateHudIndicator = gameResourceRepository.get("tank.dead", mapOf(), TextureRes, Eager), // TODO: Wrong
      ultimateIconIndex = 0,
    ))
    addModel(SimpleArmorModelCC(maxHealth = 1000))
    addModel(EngineModelCC(
      engineIdleSound = gameResourceRepository.get("tank.sound.idle", mapOf(), SoundRes, Eager),
      engineMovingSound = gameResourceRepository.get("tank.sound.moving", mapOf(), SoundRes, Eager),
      engineStartMovingSound = gameResourceRepository.get("tank.sound.start-move", mapOf(), SoundRes, Eager),
      engineStartSound = gameResourceRepository.get("tank.sound.start-move", mapOf(), SoundRes, Eager),
      engineStopMovingSound = gameResourceRepository.get("tank.sound.idle", mapOf(), SoundRes, Eager),
    ))
    addModel(marketItem.getComponent<Object3DComponent>().resource.asModel())
    addModel(HullSmokeModelCC(
      alpha = 0.5f,
      density = 1f,
      enabled = true,
      fadeTime = 1000,
      farDistance = 1000f,
      nearDistance = 1f,
      particle = gameResourceRepository.get("tank.smoke", mapOf(), MultiframeTextureRes, Eager),
      size = 1f,
    ))
    addModel(TankExplosionModelCC(
      explosionTexture = gameResourceRepository.get("tank.explosion", mapOf(), MultiframeTextureRes, Eager),
      shockWaveTexture = gameResourceRepository.get("tank.shock-wave", mapOf(), MultiframeTextureRes, Eager),
      smokeTextureId = gameResourceRepository.get("tank.smoke", mapOf(), MultiframeTextureRes, Eager),
    ))
    addModel(TrackedChassisModelCC(damping = properties.getComponent<DampingComponent>().damping))
  }
}
