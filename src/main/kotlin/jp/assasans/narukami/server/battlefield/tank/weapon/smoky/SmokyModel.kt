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

import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.net.command.ProtocolEvent
import jp.assasans.narukami.server.net.command.ProtocolModel

@ProtocolModel(2172139419072369403)
class SmokyModelCC : IModelConstructor

@ProtocolEvent(5645206942441648925)
data class SmokyModelLocalCriticalHitEvent(
  val targetId: Long,
) : IClientEvent

@ProtocolEvent(5513207377268960901)
data class SmokyModelShootEvent(
  val shooterId: Long,
) : IClientEvent

@ProtocolEvent(4100592046364094803)
data class SmokyModelShootStaticEvent(
  val shooterId: Long,
  val hitPoint: Vector3d,
) : IClientEvent

@ProtocolEvent(4100592046375670902)
data class SmokyModelShootTargetEvent(
  val shooterId: Long,
  val targetId: Long,
  val hitPoint: Vector3d,
  val weakeningCoeff: Float,
  val isCritical: Boolean
) : IClientEvent

@ProtocolEvent(682326792057158253)
data class SmokyModelFireCommandEvent(
  val clientTime: Int,
) : IServerEvent

@ProtocolEvent(7586454165429681851)
data class SmokyModelFireStaticCommandEvent(
  val clientTime: Int,
  val hitPoint: Vector3d,
) : IServerEvent

@ProtocolEvent(7267965420731948862)
data class SmokyModelFireTargetCommandEvent(
  val clientTime: Int,
  val targetId: Long,
  val targetIncarnation: Short,
  val targetPosition: Vector3d,
  val hitPoint: Vector3d,
  val hitPointWorld: Vector3d,
) : IServerEvent
