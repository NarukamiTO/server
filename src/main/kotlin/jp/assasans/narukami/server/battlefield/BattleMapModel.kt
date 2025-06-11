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

package jp.assasans.narukami.server.battlefield

import jp.assasans.narukami.server.core.IComponent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolModel
import jp.assasans.narukami.server.protocol.ProtocolStruct
import jp.assasans.narukami.server.res.*

@ProtocolModel(4693095212037357021)
data class BattleMapModelCC(
  val dustParams: DustParamsComponent,
  val dynamicShadowParams: DynamicShadowParamsComponent,
  val environmentSound: Resource<SoundRes, Eager>,
  val fogParams: FogParamsComponent,
  val gravity: Float,
  val mapResource: Resource<MapRes, Eager>,
  val skyBoxRevolutionAxis: Vector3d,
  val skyBoxRevolutionSpeed: Float,
  val skyboxSides: SkyboxSidesComponent,
  val ssaoColor: Int,
) : IModelConstructor {
  override fun getResources(): List<Resource<*, *>> {
    return listOf(
      dustParams.dustParticle,
      environmentSound,
      mapResource,
      skyboxSides.back,
      skyboxSides.bottom,
      skyboxSides.front,
      skyboxSides.left,
      skyboxSides.right,
      skyboxSides.top,
    )
  }
}

data class MapResourceComponent(
  val resource: Resource<MapRes, Eager>,
) : IComponent

data class MapEnvironmentComponent(
  val sound: Resource<SoundRes, Eager>,
) : IComponent

@ProtocolStruct
data class DustParamsComponent(
  val alpha: Float,
  val density: Float,
  val dustFarDistance: Float,
  val dustNearDistance: Float,
  val dustParticle: Resource<MultiframeTextureRes, Eager>,
  val dustSize: Float,
) : IComponent

@ProtocolStruct
data class DynamicShadowParamsComponent(
  val angleX: Float,
  val angleZ: Float,
  val lightColor: Int,
  val shadowColor: Int,
) : IComponent

@ProtocolStruct
data class FogParamsComponent(
  val alpha: Float,
  val color: Int,
  val farLimit: Float,
  val nearLimit: Float,
) : IComponent

@ProtocolStruct
data class SkyboxSidesComponent(
  val back: Resource<TextureRes, Eager>,
  val bottom: Resource<TextureRes, Eager>,
  val front: Resource<TextureRes, Eager>,
  val left: Resource<TextureRes, Eager>,
  val right: Resource<TextureRes, Eager>,
  val top: Resource<TextureRes, Eager>,
) : IComponent

data class SkyboxRotationComponent(
  val x: Float,
  val y: Float,
  val z: Float,
  val speed: Float,
) : IComponent
