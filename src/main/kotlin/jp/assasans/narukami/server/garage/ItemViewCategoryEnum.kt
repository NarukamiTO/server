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

package jp.assasans.narukami.server.garage

import jp.assasans.narukami.server.net.command.IProtocolEnum
import jp.assasans.narukami.server.net.command.ProtocolEnum

@ProtocolEnum
enum class ItemViewCategoryEnum(override val value: Int) : IProtocolEnum<Int> {
  WEAPON(0),
  ARMOR(1),
  PAINT(2),
  INVENTORY(3),
  KIT(4),
  SPECIAL(5),
  GIVEN_PRESENTS(6),
  RESISTANCE(7),
  DRONE(8),
  INVISIBLE(9),
}
