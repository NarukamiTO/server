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

package jp.assasans.narukami.server.core

import jp.assasans.narukami.server.core.impl.GameObjectV2

/**
 * Game object returned by template should be usable right away,
 * without requiring any additional components.
 */
abstract class TemplateV2 {
  protected fun TemplateV2.gameObject(id: Long): IGameObject {
    return GameObjectV2(id, this)
  }
}

abstract class PersistentTemplateV2 : TemplateV2() {
  abstract fun instantiate(id: Long): IGameObject
}
