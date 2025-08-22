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

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolModel

@ProtocolModel(777963614732509473)
data class PerformanceModelCC(
  val alertFPSRatioThreshold: Float,
  val alertFPSThreshold: Float,
  val alertMinTestTime: Float,
  val alertPingRatioThreshold: Float,
  val alertPingThreshold: Float,
  val indicatorHighFPS: Int,
  val indicatorHighFPSColor: String?,
  val indicatorHighPing: Int,
  val indicatorHighPingColor: String?,
  val indicatorLowFPS: Int,
  val indicatorLowFPSColor: String?,
  val indicatorLowPing: Int,
  val indicatorLowPingColor: String?,
  val indicatorVeryHighPing: Int,
  val indicatorVeryHighPingColor: String?,
  val indicatorVeryLowFPS: Int,
  val indicatorVeryLowFPSColor: String?,
  val qualityFPSThreshold: Float,
  val qualityIdleTime: Float,
  val qualityMaxAttempts: Int,
  val qualityRatioThreshold: Float,
  val qualityTestTime: Float,
  val qualityVisualizationSpeed: Float,
) : IModelConstructor
