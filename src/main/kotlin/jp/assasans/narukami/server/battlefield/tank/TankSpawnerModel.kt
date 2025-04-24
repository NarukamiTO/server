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

import jp.assasans.narukami.server.battlefield.BattlefieldModelCC
import jp.assasans.narukami.server.battlefield.Vector3d
import jp.assasans.narukami.server.battleselect.BattleTeam
import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.net.command.ProtocolEvent
import jp.assasans.narukami.server.net.command.ProtocolModel

@ProtocolModel(2104999573658810427)
data class TankSpawnerModelCC(
  val incarnationId: Short,
) : IModelConstructor

/**
 * Moves the camera to the set position and orientation.
 */
@ProtocolEvent(7910078009301382601)
data class TankSpawnerModelPrepareToSpawnEvent(
  val position: Vector3d,
  val orientation: Vector3d,
) : IClientEvent

@ProtocolEvent(4028620761465158251)
data class TankSpawnerModelSpawnEvent(
  val team: BattleTeam,
  val position: Vector3d,
  val orientation: Vector3d,
  val health: Short,
  val incarnationId: Short,
) : IClientEvent

/**
 * Acknowledgment of [TankSpawnerModelSpawnEvent] for local tank.
 */
@ProtocolEvent(6199716760715904819)
data class TankSpawnerModelConfirmSpawnEvent(
  val incarnationId: Short,
) : IServerEvent

/**
 * Sent whenever the tank is ready to spawn after processing [TankSpawnerModelPrepareToSpawnEvent].
 *
 * Delayed if pause is enabled and the player is [BossRelationRole.VICTIM], i.e., not the Juggernaut.
 */
@ProtocolEvent(9209552134071208874)
class TankSpawnerModelReadyToSpawnEvent : IServerEvent

/**
 * Sent whenever the tank can be activated:
 * - after [BattlefieldModelCC.respawnDuration], and
 * - not inside another active tank, and
 * - no dialogs to interfere with gameplay.
 *
 * Sent after processing [TankSpawnerModelPrepareToSpawnEvent], assuming that
 * server won't reject [TankSpawnerModelReadyToSpawnEvent].
 */
@ProtocolEvent(1601257880117586925)
class TankSpawnerModelSetReadyToPlaceEvent : IServerEvent
