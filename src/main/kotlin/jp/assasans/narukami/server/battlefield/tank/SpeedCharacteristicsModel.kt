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

package jp.assasans.narukami.server.battlefield.tank

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolModel

@ProtocolModel(7163171834328288344)
data class SpeedCharacteristicsModelCC(
  val baseAcceleration: Float,
  val baseSpeed: Float,
  val baseTurnSpeed: Float,
  val baseTurretRotationSpeed: Float,
  val currentAcceleration: Float,
  val currentSpeed: Float,
  val currentTurnSpeed: Float,
  val currentTurretRotationSpeed: Float,
  val reverseAcceleration: Float,
  val reverseTurnAcceleration: Float,
  val sideAcceleration: Float,
  val turnAcceleration: Float,
  val turnStabilizationAcceleration: Float,
) : IModelConstructor
