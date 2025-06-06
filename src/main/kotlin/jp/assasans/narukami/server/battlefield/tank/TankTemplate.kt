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

import jp.assasans.narukami.server.battlefield.BattleGearScoreModelCC
import jp.assasans.narukami.server.battlefield.BossStateModelCC
import jp.assasans.narukami.server.battlefield.UserGroupComponent
import jp.assasans.narukami.server.battlefield.tank.pause.TankPauseModelCC
import jp.assasans.narukami.server.battlefield.tank.suicide.SuicideModelCC
import jp.assasans.narukami.server.core.IModelProvider
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.net.command.ProtocolClass

@ProtocolClass(53)
data class TankTemplate(
  val userGroup: UserGroupComponent,
  val tankSpawner: TankSpawnerModelCC,
  val tankConfiguration: TankConfigurationModelCC,
  val tank: IModelProvider<TankModelCC>,
  val tankResistances: TankResistancesModelCC,
  val tankPause: TankPauseModelCC,
  val speedCharacteristics: SpeedCharacteristicsModelCC,
  val ultimate: UltimateModelCC,
  val droneIndicator: DroneIndicatorModelCC,
  val tankDevice: TankDeviceModelCC,
  val suicide: SuicideModelCC,
  val tankTemperature: TankTemperatureModelCC,
  val bossStateModel: IModelProvider<BossStateModelCC>,
  // Required for remote tanks
  val gearScoreModel: BattleGearScoreModelCC,
) : ITemplate
