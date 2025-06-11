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

package jp.assasans.narukami.server.battlefield.tank.hull

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.protocol.ProtocolModel
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.MultiframeTextureRes
import jp.assasans.narukami.server.res.Resource

@ProtocolModel(8517963146836182364)
class TankExplosionModelCC(
  val explosionTexture: Resource<MultiframeTextureRes, Eager>,
  val shockWaveTexture: Resource<MultiframeTextureRes, Eager>,
  val smokeTextureId: Resource<MultiframeTextureRes, Eager>,
) : IModelConstructor
