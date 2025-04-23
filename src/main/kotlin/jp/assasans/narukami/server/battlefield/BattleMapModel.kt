/*
 * Araumi TO - a server software reimplementation for a certain browser tank game.
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

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.net.command.ProtocolStruct
import jp.assasans.narukami.server.res.*

@ProtocolModel(4693095212037357021)
data class BattleMapModelCC(
  val dustParams: DustParams,
  val dynamicShadowParams: DynamicShadowParams,
  val environmentSound: Resource<SoundRes, Eager>,
  val fogParams: FogParams,
  val gravity: Float,
  val mapResource: Resource<MapRes, Eager>,
  val skyBoxRevolutionAxis: Vector3d,
  val skyBoxRevolutionSpeed: Float,
  val skyboxSides: SkyboxSides,
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

@ProtocolStruct
data class DustParams(
  val alpha: Float,
  val density: Float,
  val dustFarDistance: Float,
  val dustNearDistance: Float,
  val dustParticle: Resource<MultiframeTextureRes, Eager>,
  val dustSize: Float,
)

@ProtocolStruct
data class DynamicShadowParams(
  val angleX: Float,
  val angleZ: Float,
  val lightColor: Int,
  val shadowColor: Int,
)

@ProtocolStruct
data class FogParams(
  val alpha: Float,
  val color: Int,
  val farLimit: Float,
  val nearLimit: Float,
)

@ProtocolStruct
data class SkyboxSides(
  val back: Resource<TextureRes, Eager>,
  val bottom: Resource<TextureRes, Eager>,
  val front: Resource<TextureRes, Eager>,
  val left: Resource<TextureRes, Eager>,
  val right: Resource<TextureRes, Eager>,
  val top: Resource<TextureRes, Eager>,
)
