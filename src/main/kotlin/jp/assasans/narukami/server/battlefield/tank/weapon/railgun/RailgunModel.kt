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

package jp.assasans.narukami.server.battlefield.tank.weapon.railgun

import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.net.command.ProtocolEvent
import jp.assasans.narukami.server.net.command.ProtocolModel

@ProtocolModel(1663344340563853103)
data class RailgunModelCC(
  val chargingTimeMsec: Int,
  val weakeningCoeff: Float,
) : IModelConstructor

@ProtocolEvent(376004548045092294)
data class RailgunModelFireEvent(
  val shooter: IGameObject,
  val staticHitPoint: Vector3d?,
  val targets: List<IGameObject>?,
  val targetHitPoints: List<Vector3d>?,
) : IClientEvent

@ProtocolEvent(1244736124634194290)
data class RailgunModelFireDummyEvent(
  val shooter: IGameObject,
) : IClientEvent

@ProtocolEvent(6177248960642588198)
class RailgunModelImmediateReloadEvent : IClientEvent

@ProtocolEvent(2359710046028200821)
data class RailgunModelReconfigureWeaponEvent(
  val chargingTime: Int,
) : IClientEvent

@ProtocolEvent(2141301983650645649)
data class RailgunModelStartChargingEvent(
  val shooter: IGameObject,
) : IClientEvent

@ProtocolEvent(1925343334566600919)
data class RailgunModelFireCommandEvent(
  val clientTime: Int,
  val staticHitPoint: Vector3d?,
  val targets: List<IGameObject>?,
  val targetHitPoints: List<Vector3d>?,
  val targetIncarnations: List<Short>?,
  val targetPositions: List<Vector3d>?,
  val hitPointsWorld: List<Vector3d>?,
) : IServerEvent

@ProtocolEvent(7694243472391102363)
data class RailgunModelFireDummyCommandEvent(
  val clientTime: Int,
) : IServerEvent

@ProtocolEvent(3209441135223108196)
data class RailgunModelStartChargingCommandEvent(
  val clientTime: Int,
) : IServerEvent
