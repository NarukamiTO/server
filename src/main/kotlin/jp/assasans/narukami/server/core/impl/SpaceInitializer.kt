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
import jp.assasans.narukami.server.ColorAdjustModelCC
import jp.assasans.narukami.server.ColorAdjustParams
import jp.assasans.narukami.server.battlefield.*
import jp.assasans.narukami.server.battleselect.*
import jp.assasans.narukami.server.battleservice.StatisticsDMModel
import jp.assasans.narukami.server.battleservice.StatisticsModelCC
import jp.assasans.narukami.server.battleservice.UserInfo
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherModelCC
import jp.assasans.narukami.server.entrance.EntranceTemplate
import jp.assasans.narukami.server.lobby.LobbyTemplate
import jp.assasans.narukami.server.lobby.communication.ChatModeratorLevel
import jp.assasans.narukami.server.lobby.communication.CommunicationTemplate
import jp.assasans.narukami.server.lobby.user.RankLoaderTemplate
import jp.assasans.narukami.server.res.*

data class RegistrationBackgroundComponent(val resource: Resource<ImageRes, *>) : IComponent
data class RegistrationPasswordLimitsComponent(
  val minPasswordLength: Int,
  val maxPasswordLength: Int,
) : IComponent

class SpaceInitializer(
  private val spaces: IRegistry<ISpace>
) : IEvent, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  fun init() {
    spaces.add(Space(0xaa55).apply {
      logger.info { "EntranceClass models: ${EntranceTemplate::class.models}" }

      val entranceClass = TemplatedGameClass.fromTemplate(EntranceTemplate::class)
      val entranceObject = TransientGameObject.instantiate(
        id = 2,
        entranceClass,
        EntranceTemplate.Provider.create(),
        setOf(
          RegistrationBackgroundComponent(gameResourceRepository.get("entrance.background", emptyMap(), ImageRes, Eager)),
          RegistrationPasswordLimitsComponent(minPasswordLength = 6, maxPasswordLength = 20)
        )
      )

      objects.add(entranceObject)
    })

    spaces.add(Space(0x55aa).apply {
      val lobbyClass = TemplatedGameClass.fromTemplate(LobbyTemplate::class)
      val lobbyObject = TransientGameObject.instantiate(
        id = 2,
        lobbyClass,
        LobbyTemplate.Provider.create()
      )
      objects.add(lobbyObject)

      val rankLoaderObject = TransientGameObject.instantiate(
        id = 8,
        parent = TemplatedGameClass.fromTemplate(RankLoaderTemplate::class),
        RankLoaderTemplate.Provider.create()
      )
      objects.add(rankLoaderObject)

      val communicationClass = TemplatedGameClass.fromTemplate(CommunicationTemplate::class)
      val communicationObject = TransientGameObject.instantiate(
        5,
        communicationClass,
        CommunicationTemplate.Provider.create()
      )
      objects.add(communicationObject)

      val battleSelectClass = TemplatedGameClass.fromTemplate(BattleSelectTemplate::class)
      val battleSelectObject = TransientGameObject.instantiate(
        6,
        battleSelectClass,
        BattleSelectTemplate.Provider.create()
      )
      objects.add(battleSelectObject)

      val battleCreateClass = TemplatedGameClass.fromTemplate(BattleCreateTemplate::class)
      val battleCreateObject = TransientGameObject.instantiate(
        10,
        battleCreateClass,
        BattleCreateTemplate.Provider.create()
      )
      objects.add(battleCreateObject)

      val mapInfoClass = TemplatedGameClass.fromTemplate(MapInfoTemplate::class)
      val mapInfoObject = TransientGameObject.instantiate(
        7,
        mapInfoClass,
        MapInfoTemplate.Provider.create()
      ).also { objects.add(it) }

      TransientGameObject.instantiate(
        11,
        mapInfoClass,
        MapInfoTemplate(
          mapInfo = MapInfoModelCC(
            defaultTheme = MapTheme.SUMMER_NIGHT,
            enabled = true,
            mapId = 11,
            mapName = "Spawn Test 2",
            matchmakingMark = false,
            maxPeople = 32,
            preview = gameResourceRepository.get("entrance.background", emptyMap(), ImageRes, Lazy),
            rankLimit = Range(min = 1, max = 31),
            supportedModes = listOf(
              BattleMode.DM,
            ),
            theme = MapTheme.SUMMER_NIGHT
          )
        )
      ).also { objects.add(it) }

      val battleInfoClass = TemplatedGameClass.fromTemplate(DMBattleInfoTemplate::class)
      val battleInfoObject = TransientGameObject.instantiate(
        9,
        battleInfoClass,
        DMBattleInfoTemplate(
          common = BattleInfoTemplate(
            battleInfo = BattleInfoModelCC(
              roundStarted = false,
              suspicionLevel = BattleSuspicionLevel.NONE,
              timeLeftInSec = 3600,
            ),
            battleParamInfo = BattleParamInfoModelCC(
              map = mapInfoObject,
              params = BattleCreateParameters(
                autoBalance = false,
                battleMode = BattleMode.DM,
                clanBattle = false,
                dependentCooldownEnabled = false,
                equipmentConstraintsMode = null,
                friendlyFire = false,
                goldBoxesEnabled = false,
                limits = BattleLimits(scoreLimit = 0, timeLimitInSec = 0),
                mapId = mapInfoObject.id,
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
            ),
            battleEntrance = BattleEntranceModelCC(),
          ),
          battleDMInfo = BattleDMInfoModelCC(
            users = listOf()
          )
        )
      )
      objects.add(battleInfoObject)
    })

    spaces.add(Space(4242).apply {
      objects.remove(rootObject)

      val battleMapClass = TemplatedGameClass.fromTemplate(BattleMapTemplate::class)
      val battleMapObject = TransientGameObject.instantiate(
        4200,
        battleMapClass,
        BattleMapTemplate(
          battleMap = BattleMapModelCC(
            dustParams = DustParams(
              alpha = 0.75f,
              density = 0.15f,
              dustFarDistance = 7000.0f,
              dustNearDistance = 5000.0f,
              dustParticle = gameResourceRepository.get("battle.dust", mapOf("theme" to "summer"), MultiframeTextureRes, Eager),
              dustSize = 180.0f
            ),
            dynamicShadowParams = DynamicShadowParams(angleX = -1.0f, angleZ = 0.5f, lightColor = 13090219, shadowColor = 5530735),
            environmentSound = gameResourceRepository.get("battle.sound.ambience", mapOf(), SoundRes, Eager),
            fogParams = FogParams(alpha = 0.25f, color = 14545407, farLimit = 10000.0f, nearLimit = 5000.0f),
            gravity = 1000.0f,
            mapResource = gameResourceRepository.get(
              "map.spawn-test",
              mapOf(
                "gen" to "2.1",
                "variant" to "default",
              ),
              MapRes,
              Eager
            ),
            skyBoxRevolutionAxis = Vector3d(x = 0.0f, y = 0.0f, z = 0.0f),
            skyBoxRevolutionSpeed = 0.0f,
            skyboxSides = SkyboxSides(
              back = gameResourceRepository.get("skybox.mountains.back", mapOf("gen" to "1.0"), TextureRes, Eager),
              bottom = gameResourceRepository.get("skybox.mountains.bottom", mapOf("gen" to "1.0"), TextureRes, Eager),
              front = gameResourceRepository.get("skybox.mountains.front", mapOf("gen" to "1.0"), TextureRes, Eager),
              left = gameResourceRepository.get("skybox.mountains.left", mapOf("gen" to "1.0"), TextureRes, Eager),
              right = gameResourceRepository.get("skybox.mountains.right", mapOf("gen" to "1.0"), TextureRes, Eager),
              top = gameResourceRepository.get("skybox.mountains.top", mapOf("gen" to "1.0"), TextureRes, Eager),
            ),
            ssaoColor = 3025184
          ),
          colorAdjust = ColorAdjustModelCC(
            frostParamsHW = ColorAdjustParams(1f, 0f, 1.5f, 100f, 1f, 80f, 1f, 20f),
            frostParamsSoft = ColorAdjustParams(1f, 0f, 1.5f, 100f, 1f, 80f, 1f, 20f),
            heatParamsHW = ColorAdjustParams(1f, 0f, 0.6f, -40f, 0.6f, -20f, 1.5f, 40f),
            heatParamsSoft = ColorAdjustParams(1f, 0f, 0.6f, -40f, 0.6f, -20f, 1.5f, 40f),
          ),
          mapBonusLight = MapBonusLightModelCC(
            bonusLightIntensity = 0f,
            hwColorAdjust = ColorAdjustParams(1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f),
            softColorAdjust = ColorAdjustParams(1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f),
          )
        )
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
