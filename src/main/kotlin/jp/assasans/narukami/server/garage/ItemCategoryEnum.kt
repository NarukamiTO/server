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

import jp.assasans.narukami.server.protocol.IProtocolEnum
import jp.assasans.narukami.server.protocol.ProtocolEnum

@ProtocolEnum
enum class ItemCategoryEnum(override val value: Int) : IProtocolEnum<Int> {
  WEAPON(0),
  ARMOR(1),
  PAINT(2),
  INVENTORY(3),
  PLUGIN(4),
  KIT(5),
  EMBLEM(6),
  CRYSTAL(7),
  PRESENT(8),
  GIVEN_PRESENT(9),
  RESISTANCE_MODULE(10),
  DEVICE(11),
  LICENSE(12),
  CONTAINER(13),
  DRONE(14),
  SKIN(15),
  MOBILE_LOOT_BOX(16),
}
