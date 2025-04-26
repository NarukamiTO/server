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

package jp.assasans.narukami.server.core.impl

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.*
import jp.assasans.narukami.server.battleselect.BattleLimits
import jp.assasans.narukami.server.battleselect.Range
import jp.assasans.narukami.server.battleservice.StatisticsDMModel
import jp.assasans.narukami.server.battleservice.StatisticsModelCC
import jp.assasans.narukami.server.battleservice.UserInfo
import jp.assasans.narukami.server.core.IEvent
import jp.assasans.narukami.server.core.IRegistry
import jp.assasans.narukami.server.core.ISpace
import jp.assasans.narukami.server.core.StaticModelProvider
import jp.assasans.narukami.server.dispatcher.DispatcherModelCC
import jp.assasans.narukami.server.lobby.communication.ChatModeratorLevel
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.RemoteGameResourceRepository
import jp.assasans.narukami.server.res.SoundRes

class SpaceInitializer(
  private val spaces: IRegistry<ISpace>
) : IEvent, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  fun init() {
    spaces.add(Space(Space.stableId("entrance")))
    spaces.add(Space(Space.stableId("lobby")))

    spaces.add(Space(4242).apply {
      objects.remove(rootObject)

      val battleMapClass = TemplatedGameClass.fromTemplate(BattleMapTemplate::class)
      val battleMapObject = TransientGameObject.instantiate(
        4200,
        battleMapClass,
        BattleMapTemplate.Provider.create()
      )
      objects.add(battleMapObject)

      val battlefieldClass = TemplatedGameClass.fromTemplate(BattlefieldTemplate::class)
      val battlefieldObject = TransientGameObject.instantiate(
        4242,
        battlefieldClass,
        BattlefieldTemplate(
          battlefield = BattlefieldModelCC(
            active = true,
            battleId = 4242,
            battlefieldSounds = BattlefieldSounds(
              battleFinishSound = gameResourceRepository.get("battle.sound.finish", mapOf(), SoundRes, Eager),
              killSound = gameResourceRepository.get("tank.sound.destroy", mapOf(), SoundRes, Eager)
            ),
            colorTransformMultiplier = 0.0f,
            idleKickPeriodMsec = 0,
            map = battleMapObject,
            mineExplosionLighting = LightingSFXEntity(effects = listOf()),
            proBattle = false,
            range = Range(min = 1, max = 31),
            reArmorEnabled = false,
            respawnDuration = 1000,
            shadowMapCorrectionFactor = 0.0f,
            showAddressLink = false,
            spectator = false,
            withoutBonuses = false,
            withoutDrones = false,
            withoutSupplies = false
          ),
          battlefieldBonuses = BattlefieldBonusesModelCC(
            bonusFallSpeed = 0.0f,
            bonuses = listOf()
          ),
          battleFacilities = BattleFacilitiesModelCC(),
          battleChat = BattleChatModelCC(),
          statistics = StatisticsModelCC(
            battleName = null,
            equipmentConstraintsMode = null,
            fund = 42,
            limits = BattleLimits(scoreLimit = 0, timeLimitInSec = 0),
            mapName = "BattlefieldTemplate",
            matchBattle = false,
            maxPeopleCount = 0,
            modeName = "DM",
            parkourMode = false,
            running = true,
            spectator = false,
            suspiciousUserIds = listOf(),
            timeLeft = (21.minutes + 12.seconds).inWholeSeconds.toInt(),
            valuableRound = true
          ),
          statisticsDM = StatisticsDMModel(
            usersInfo = listOf(
              UserInfo(
                chatModeratorLevel = ChatModeratorLevel.ADMINISTRATOR,
                deaths = 0,
                hasPremium = false,
                kills = 0,
                rank = 1,
                score = 0,
                uid = "Player",
                user = 30,
              )
            )
          ),
          inventory = InventoryModelCC(ultimateEnabled = false),
          battleDM = BattleDMModelCC(),
        )
      )
      battlefieldObject.models[DispatcherModelCC::class] = StaticModelProvider(DispatcherModelCC())
      objects.add(battlefieldObject)
    })
  }
}
