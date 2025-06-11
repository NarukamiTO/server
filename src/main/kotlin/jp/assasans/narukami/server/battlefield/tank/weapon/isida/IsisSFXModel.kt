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

package jp.assasans.narukami.server.battlefield.tank.weapon.isida

import jp.assasans.narukami.server.battlefield.LightingSFXEntity
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolModel
import jp.assasans.narukami.server.res.*

@ProtocolModel(5745114335268581493)
data class IsisSFXModelCC(
  val damagingBall: Resource<MultiframeTextureRes, Eager>,
  val damagingRay: Resource<TextureRes, Eager>,
  val damagingSound: Resource<SoundRes, Eager>,
  val healingBall: Resource<MultiframeTextureRes, Eager>,
  val healingRay: Resource<TextureRes, Eager>,
  val healingSound: Resource<SoundRes, Eager>,
  val idleSound: Resource<SoundRes, Eager>,
  val lightingSFXEntity: LightingSFXEntity,
) : IModelConstructor
