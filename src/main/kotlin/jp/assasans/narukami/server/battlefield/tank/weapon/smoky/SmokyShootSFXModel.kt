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

package jp.assasans.narukami.server.battlefield.tank.weapon.smoky

import jp.assasans.narukami.server.battlefield.LightingSFXEntity
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.res.*

@ProtocolModel(1748249573394295263)
class SmokyShootSFXModelCC(
  val criticalHitSize: Short,
  val criticalHitTexture: Resource<MultiframeTextureRes, Eager>,
  val explosionMarkTexture: Resource<TextureRes, Eager>,
  val explosionSize: Short,
  val explosionSound: Resource<SoundRes, Eager>,
  val explosionTexture: Resource<MultiframeTextureRes, Eager>,
  val lightingSFXEntity: LightingSFXEntity,
  val shotSound: Resource<SoundRes, Eager>,
  val shotTexture: Resource<TextureRes, Eager>,
) : IModelConstructor
