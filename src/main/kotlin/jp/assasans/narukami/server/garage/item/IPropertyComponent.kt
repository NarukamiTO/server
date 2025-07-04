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

package jp.assasans.narukami.server.garage.item

import jp.assasans.narukami.server.core.IComponent

interface IPropertyComponent : IComponent

data class ImpactForceComponent(val impactForce: Float) : IPropertyComponent
data class KickbackComponent(val kickback: Float) : IPropertyComponent
data class TurretRotationSpeedComponent(val turretRotationSpeed: Float) : IPropertyComponent

data class MassComponent(val mass: Float) : IPropertyComponent
data class DampingComponent(val damping: Float) : IPropertyComponent

data class SpeedComponent(
  val speed: Float,
  val turnSpeed: Float,
) : IPropertyComponent

data class AccelerationComponent(
  val forwardAcceleration: Float,
  val reverseAcceleration: Float,
  val forwardTurnAcceleration: Float,
  val reverseTurnAcceleration: Float,
  val sideAcceleration: Float,
  val turnStabilizationAcceleration: Float,
) : IPropertyComponent
