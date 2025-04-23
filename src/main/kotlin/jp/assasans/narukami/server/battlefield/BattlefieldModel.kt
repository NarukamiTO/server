/*
 * Araumi TO - a server software reimplementation for a certain browser tank game.
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
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.net.command.ProtocolStruct
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

@ProtocolStruct
data class LightingSFXEntity(
  val effects: List<LightingEffectEntity>,
)

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
