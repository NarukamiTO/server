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

package jp.assasans.narukami.server.battlefield.damage

import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.*

@ProtocolModel(3248199557469599724)
class DamageIndicatorModelCC : IModelConstructor

@ProtocolEvent(1597293699677435224)
data class DamageIndicatorModelShowDamageForShooterEvent(
  val damages: List<TargetTankDamage>,
) : IModelConstructor

@ProtocolStruct
data class TargetTankDamage(
  val target: IGameObject,
  val damageAmount: Float,
  val damageIndicatorType: DamageIndicatorType,
)

@ProtocolEnum
enum class DamageIndicatorType(override val value: Int) : IProtocolEnum<Int> {
  NORMAL(0),
  CRITICAL(1),
  FATAL(2),
  HEAL(3),
}
