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

package jp.assasans.narukami.server.battleselect

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import jp.assasans.narukami.server.battlefield.BattleUserComponent
import jp.assasans.narukami.server.battlefield.BattleUserNode
import jp.assasans.narukami.server.battlefield.asBattleInfoUser
import jp.assasans.narukami.server.core.*

abstract class BattleInfoTemplate : TemplateV2() {
  open fun create(id: Long, mapInfo: IGameObject) = gameObject(id).apply {
    addModel(
      BattleInfoModelCC(
        roundStarted = false,
        suspicionLevel = BattleSuspicionLevel.NONE,
        timeLeftInSec = 3600,
      )
    )
    addModel(
      BattleParamInfoModelCC(
        map = mapInfo,
        params = BattleCreateParameters(
          autoBalance = false,
          battleMode = BattleMode.DM,
          clanBattle = false,
          dependentCooldownEnabled = false,
          equipmentConstraintsMode = null,
          friendlyFire = false,
          goldBoxesEnabled = false,
          limits = BattleLimits(
            scoreLimit = 999,
            timeLimitInSec = (21.minutes + 12.seconds).inWholeSeconds.toInt()
          ),
          mapId = mapInfo.id,
          maxPeopleCount = 32,
          name = null,
          parkourMode = false,
          privateBattle = false,
          proBattle = true,
          rankRange = Range(min = 1, max = 31),
          reArmorEnabled = true,
          theme = MapTheme.SUMMER_NIGHT,
          ultimatesEnabled = false,
          uniqueUsersBattle = false,
          withoutBonuses = false,
          withoutDevices = false,
          withoutDrones = false,
          withoutSupplies = false,
          withoutUpgrades = false,
        )
      )
    )
    addModel(BattleEntranceModelCC())
  }
}

object DMBattleInfoTemplate : BattleInfoTemplate() {
  override fun create(id: Long, mapInfo: IGameObject) = super.create(id, mapInfo).apply {
    addModel(ClosureModelProvider {
      val battleUsers = it.getComponent<BattleSpaceComponent>()
        .space.objects.all
        .filterHasComponent<BattleUserComponent>()
        .map { it.adapt<BattleUserNode>() }
        .filter { it.team != null }
      BattleDMInfoModelCC(
        users = battleUsers.map { battleUser -> battleUser.asBattleInfoUser() }
      )
    })
  }
}
