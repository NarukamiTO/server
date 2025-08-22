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

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.chat.BattleChatModelCC
import jp.assasans.narukami.server.battlefield.damage.DamageIndicatorModelCC
import jp.assasans.narukami.server.battleselect.BattleLimits
import jp.assasans.narukami.server.battleselect.Range
import jp.assasans.narukami.server.battleservice.StatisticsDMModel
import jp.assasans.narukami.server.battleservice.StatisticsModelCC
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.RemoteGameResourceRepository
import jp.assasans.narukami.server.res.SoundRes

object BattlefieldTemplate : TemplateV2(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  fun create(id: Long, battleMapObject: IGameObject) = gameObject(id).apply {
    addModel(ClosureModelProvider {
      // TODO: Design of model providers is broken
      val fakeUser = VoidNode()
      fakeUser.init(this, gameObject(0).apply {
        addComponent(requireSpaceChannel.sessionNotNull.userNotNull.getComponent<UserGroupComponent>())
      })
      val battleUser = space.objects.all.findBy<BattleUserNode, UserGroupComponent>(fakeUser)

      BattlefieldModelCC(
        active = true,
        battleId = id,
        battlefieldSounds = BattlefieldSounds(
          battleFinishSound = gameResourceRepository.get("battle.sound.finish", mapOf(), SoundRes, Eager),
          killSound = gameResourceRepository.get("tank.sound.destroy", mapOf(), SoundRes, Eager)
        ),
        colorTransformMultiplier = 1.0f,
        idleKickPeriodMsec = 5.minutes.inWholeMilliseconds.toInt(),
        map = battleMapObject,
        mineExplosionLighting = LightingSFXEntity(
          effects = listOf(
            LightingEffectEntity(
              "explosion", listOf(
                LightEffectItem(300f, 0f, "0xff0000", 0.5f, 0),
                LightEffectItem(1f, 2f, "0xff0000", 0f, 500)
              )
            )
          )
        ),
        proBattle = true,
        range = Range(min = 1, max = 31),
        reArmorEnabled = false,
        respawnDuration = 2000,
        shadowMapCorrectionFactor = 0.0f,
        showAddressLink = false,
        spectator = battleUser.spectator != null,
        withoutBonuses = false,
        withoutDrones = false,
        withoutSupplies = false
      )
    })
    addModel(
      BattlefieldBonusesModelCC(
        bonusFallSpeed = 0.0f,
        bonuses = listOf()
      )
    )
    addModel(BattleFacilitiesModelCC())
    addModel(BattleChatModelCC())

    // Absence of [StatisticsModel] causes an unhelpful error, pointing to [TankModel#registerUser].
    addModel(ClosureModelProvider {
      // TODO: Design of model providers is broken
      val fakeUser = VoidNode()
      fakeUser.init(this, gameObject(0).apply {
        addComponent(requireSpaceChannel.sessionNotNull.userNotNull.getComponent<UserGroupComponent>())
      })
      val battleUser = space.objects.all.findBy<BattleUserNode, UserGroupComponent>(fakeUser)

      StatisticsModelCC(
        battleName = null,
        equipmentConstraintsMode = null,
        fund = 42,
        limits = BattleLimits(
          scoreLimit = 999,
          timeLimitInSec = (21.minutes + 12.seconds).inWholeSeconds.toInt()
        ),
        mapName = "BattlefieldTemplate",
        matchBattle = false,
        maxPeopleCount = 0,
        modeName = "DM",
        parkourMode = false,
        running = true,
        spectator = battleUser.spectator != null,
        suspiciousUserIds = listOf(),
        timeLeft = (21.minutes + 12.seconds).inWholeSeconds.toInt(),
        valuableRound = true
      )
    })
    addModel(ClosureModelProvider {
      val tanks = space.objects.all.filter { it.hasComponent<TankLogicStateComponent>() }
      StatisticsDMModel(
        usersInfo = space.objects.all
          .filterHasComponent<BattleUserComponent>()
          .map { it.adapt<BattleUserNode>() }
          // Exclude battle users that have no tank object
          // TODO: Workaround, works for now
          .filter { battleUser -> tanks.any { it.getComponent<UserGroupComponent>() == battleUser.userGroup } }
          .map { it.asUserInfo(space.objects) }
      )
    })

    // Absence of [InventoryModel] causes an unhelpful error, pointing to [DroneIndicatorModel#updateBatteryIndicator].
    addModel(InventoryModelCC(ultimateEnabled = false))
    // TODO: InventorySfxModelCC
    // TODO: ContinueBattleModelCC
    addModel(DamageIndicatorModelCC())

    addModel(BattleDMModelCC())

    // Fatal error on Standalone Flash Player
    addModel(
      PerformanceModelCC(
        alertFPSRatioThreshold = 0.9f,
        alertFPSThreshold = 10000f,
        alertMinTestTime = 10000f,
        alertPingRatioThreshold = 0.9f,
        alertPingThreshold = 900f,

        indicatorHighFPS = 50,
        indicatorHighFPSColor = "0x00ff00",
        indicatorHighPing = 200,
        indicatorHighPingColor = "0xaaaa00",
        indicatorLowFPS = 30,
        indicatorLowFPSColor = "0xaaaa00",
        indicatorLowPing = 50,
        indicatorLowPingColor = "0x00ff00",
        indicatorVeryHighPing = 900,
        indicatorVeryHighPingColor = "0xff0000",
        indicatorVeryLowFPS = 10,
        indicatorVeryLowFPSColor = "0xff0000",

        qualityFPSThreshold = 40f,
        qualityIdleTime = 1000f,
        qualityMaxAttempts = 10,
        qualityRatioThreshold = 0.5f,
        qualityTestTime = 1000f,
        qualityVisualizationSpeed = 1000f,
      )
    )
  }
}
