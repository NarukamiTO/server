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

import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.MultiframeTextureRes
import jp.assasans.narukami.server.res.Resource
import jp.assasans.narukami.server.res.TextureRes

data class StaticColoringComponent(val resource: Resource<TextureRes, Eager>) : IComponent
data class AnimatedColoringComponent(val resource: Resource<MultiframeTextureRes, Eager>) : IComponent

object PaintTemplate : TemplateV2() {
  fun create(id: Long, marketItem: IGameObject) = gameObject(id).apply {
    val staticColoring = marketItem.getComponentOrNull<StaticColoringComponent>()
    val animatedColoring = marketItem.getComponentOrNull<AnimatedColoringComponent>()
    require(staticColoring != null || animatedColoring != null) { "At least one of StaticColoringComponent or AnimatedColoringComponent must be present" }
    require(staticColoring == null || animatedColoring == null) { "Cannot have both StaticColoringComponent and AnimatedColoringComponent" }

    addModel(
      ColoringModelCC(
        animatedColoring = animatedColoring?.resource,
        coloring = staticColoring?.resource
      )
    )
  }
}
