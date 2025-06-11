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

import jp.assasans.narukami.server.battleselect.Range
import jp.assasans.narukami.server.core.IClientEvent
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.IServerEvent
import jp.assasans.narukami.server.protocol.*
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.Resource
import jp.assasans.narukami.server.res.SoundRes

@ProtocolModel(7401419333842694749)
data class BattlefieldModelCC(
  val active: Boolean,
  val battleId: Long,
  val battlefieldSounds: BattlefieldSounds,
  val colorTransformMultiplier: Float,
  val idleKickPeriodMsec: Int,
  val map: IGameObject,
  val mineExplosionLighting: LightingSFXEntity,
  val proBattle: Boolean,
  val range: Range,
  val reArmorEnabled: Boolean,
  val respawnDuration: Int,
  val shadowMapCorrectionFactor: Float,
  val showAddressLink: Boolean,
  val spectator: Boolean,
  val withoutBonuses: Boolean,
  val withoutDrones: Boolean,
  val withoutSupplies: Boolean,
) : IModelConstructor {
  override fun getResources(): List<Resource<*, *>> {
    return listOf(
      battlefieldSounds.battleFinishSound,
      battlefieldSounds.killSound,
    )
  }
}

@ProtocolEvent(5523262832935222243)
class BattlefieldModelBattleFinishEvent() : IClientEvent

@ProtocolEvent(5200451168146380991)
class BattlefieldModelBattleRestartEvent : IClientEvent

@ProtocolEvent(773226029258926866)
data class BattlefieldModelBattleStartEvent(val params: BattleRoundParameters) : IClientEvent

@ProtocolEvent(3057042862073659526)
class BattlefieldModelTraceHitEvent(val hitTraceData: HitTraceData) : IClientEvent

/**
 * Speed hack report event.
 */
@ProtocolEvent(1342713417991483261)
data class BattlefieldModelDgEvent(val deltas: List<Int>) : IServerEvent

/**
 * Data validation report event.
 */
@ProtocolEvent(1342713417991483047)
data class BattlefieldModelKdEvent(val type: Int) : IServerEvent

/**
 * Sent every 60 seconds and on battle finish.
 */
@ProtocolEvent(7686916658208568653)
data class BattlefieldModelSendTimeStatisticsCommandEvent(
  val statisticType: FpsStatisticType,
  val averageFPS: Float,
) : IServerEvent

/**
 * Unused report event.
 */
@ProtocolEvent(1342713417991482645)
class BattlefieldModelXcEvent : IServerEvent

@ProtocolEnum
enum class FpsStatisticType(override val value: Int) : IProtocolEnum<Int> {
  SOFTWARE(0),
  HARDWARE_CONSTRAINT(1),
  HARDWARE_BASELINE(2),
}

@ProtocolStruct
data class BattleRoundParameters(
  val reArmorEnabled: Boolean,
)

@ProtocolStruct
data class HitTraceData(
  val armorPreEffectDamage: Float,
  val colorResistDamage: Float,
  val hullResistDamage: Float,
  val killerTurretName: String,
  val origDamage: Float,
  val postHealth: Float,
  val targetHealth: Float,
  val targetHullName: String,
  val weaponEffectsDamage: Float,
)

@ProtocolStruct
data class LightingSFXEntity(
  val effects: List<LightingEffectEntity>,
) {
  constructor(vararg effects: Pair<String, List<LightEffectItem>>) : this(
    effects.map { LightingEffectEntity(it.first, it.second) }
  )
}

@ProtocolStruct
data class LightingEffectEntity(
  val effectName: String,
  val items: List<LightEffectItem>,
)

@ProtocolStruct
data class LightEffectItem(
  val attenuationBegin: Float,
  val attenuationEnd: Float,
  val color: String,
  val intensity: Float,
  val time: Int,
)

@ProtocolStruct
data class BattlefieldSounds(
  val battleFinishSound: Resource<SoundRes, Eager>,
  val killSound: Resource<SoundRes, Eager>,
)
