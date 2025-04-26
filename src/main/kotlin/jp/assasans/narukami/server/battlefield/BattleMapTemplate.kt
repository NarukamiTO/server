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

import jp.assasans.narukami.server.core.ClosureModelProvider
import jp.assasans.narukami.server.core.IModelProvider
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.core.ITemplateProvider
import jp.assasans.narukami.server.lobby.user.adaptSingle
import jp.assasans.narukami.server.net.command.ProtocolClass

@ProtocolClass(4200)
data class BattleMapTemplate(
  val battleMap: IModelProvider<BattleMapModelCC>,
  val colorAdjust: IModelProvider<ColorAdjustModelCC>,
  val mapBonusLight: IModelProvider<MapBonusLightModelCC>,
) : ITemplate {
  companion object {
    val Provider = ITemplateProvider {
      BattleMapTemplate(
        battleMap = ClosureModelProvider {
          val skyboxRotation = it.adaptSingle<SkyboxRotationComponent>()

          BattleMapModelCC(
            dustParams = it.adaptSingle<DustParamsComponent>(),
            dynamicShadowParams = it.adaptSingle<DynamicShadowParamsComponent>(),
            environmentSound = it.adaptSingle<MapEnvironmentComponent>().sound,
            fogParams = it.adaptSingle<FogParamsComponent>(),
            gravity = 1000.0f,
            mapResource = it.adaptSingle<MapResourceComponent>().resource,
            skyBoxRevolutionAxis = Vector3d(skyboxRotation.x, skyboxRotation.y, skyboxRotation.z),
            skyBoxRevolutionSpeed = skyboxRotation.speed,
            skyboxSides = it.adaptSingle<SkyboxSidesComponent>(),
            ssaoColor = 3025184
          )
        },
        colorAdjust = ClosureModelProvider {
          ColorAdjustModelCC(
            frostParamsHW = ColorAdjustParams(1f, 0f, 1.5f, 100f, 1f, 80f, 1f, 20f),
            frostParamsSoft = ColorAdjustParams(1f, 0f, 1.5f, 100f, 1f, 80f, 1f, 20f),
            heatParamsHW = ColorAdjustParams(1f, 0f, 0.6f, -40f, 0.6f, -20f, 1.5f, 40f),
            heatParamsSoft = ColorAdjustParams(1f, 0f, 0.6f, -40f, 0.6f, -20f, 1.5f, 40f),
          )
        },
        mapBonusLight = ClosureModelProvider {
          MapBonusLightModelCC(
            bonusLightIntensity = 0f,
            hwColorAdjust = ColorAdjustParams(1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f),
            softColorAdjust = ColorAdjustParams(1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f),
          )
        }
      )
    }
  }
}
