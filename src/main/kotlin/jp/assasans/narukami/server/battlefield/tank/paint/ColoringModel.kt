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

package jp.assasans.narukami.server.battlefield.tank.paint

import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.MultiframeTextureRes
import jp.assasans.narukami.server.res.Resource
import jp.assasans.narukami.server.res.TextureRes

@ProtocolModel(4068618447512888231)
data class ColoringModelCC(
  val animatedColoring: Resource<MultiframeTextureRes, Eager>?,
  val coloring: Resource<TextureRes, Eager>?,
) : IModelConstructor {
  companion object {
    fun animated(resource: Resource<MultiframeTextureRes, Eager>) = ColoringModelCC(
      animatedColoring = resource,
      coloring = null
    )

    fun static(resource: Resource<TextureRes, Eager>) = ColoringModelCC(
      animatedColoring = null,
      coloring = resource
    )
  }
}
