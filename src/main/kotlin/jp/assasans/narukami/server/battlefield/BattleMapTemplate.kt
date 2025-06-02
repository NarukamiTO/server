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

import jp.assasans.narukami.server.core.*
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
          val skyboxRotation = it.getComponent<SkyboxRotationComponent>()

          BattleMapModelCC(
            dustParams = it.getComponent<DustParamsComponent>(),
            dynamicShadowParams = it.getComponent<DynamicShadowParamsComponent>(),
            environmentSound = it.getComponent<MapEnvironmentComponent>().sound,
            fogParams = it.getComponent<FogParamsComponent>(),
            gravity = 1000.0f,
            mapResource = it.getComponent<MapResourceComponent>().resource,
            skyBoxRevolutionAxis = Vector3d(skyboxRotation.x, skyboxRotation.y, skyboxRotation.z),
            skyBoxRevolutionSpeed = skyboxRotation.speed,
            skyboxSides = it.getComponent<SkyboxSidesComponent>(),
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
