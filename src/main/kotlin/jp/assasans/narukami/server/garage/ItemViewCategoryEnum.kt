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

fun ItemCategoryEnum.toViewCategory() = when(this) {
  ItemCategoryEnum.WEAPON            -> ItemViewCategoryEnum.WEAPON
  ItemCategoryEnum.ARMOR             -> ItemViewCategoryEnum.ARMOR
  ItemCategoryEnum.PAINT             -> ItemViewCategoryEnum.PAINT
  ItemCategoryEnum.INVENTORY         -> ItemViewCategoryEnum.INVENTORY
  ItemCategoryEnum.PLUGIN            -> ItemViewCategoryEnum.INVISIBLE // Unknown
  ItemCategoryEnum.KIT               -> ItemViewCategoryEnum.KIT
  ItemCategoryEnum.EMBLEM            -> ItemViewCategoryEnum.INVISIBLE // Unknown
  ItemCategoryEnum.CRYSTAL           -> ItemViewCategoryEnum.INVISIBLE // Unknown
  ItemCategoryEnum.PRESENT           -> ItemViewCategoryEnum.SPECIAL
  ItemCategoryEnum.GIVEN_PRESENT     -> ItemViewCategoryEnum.GIVEN_PRESENTS
  ItemCategoryEnum.RESISTANCE_MODULE -> ItemViewCategoryEnum.RESISTANCE
  ItemCategoryEnum.DEVICE            -> ItemViewCategoryEnum.INVISIBLE // Different UI
  ItemCategoryEnum.LICENSE           -> ItemViewCategoryEnum.SPECIAL
  ItemCategoryEnum.CONTAINER         -> ItemViewCategoryEnum.SPECIAL
  ItemCategoryEnum.DRONE             -> ItemViewCategoryEnum.DRONE
  ItemCategoryEnum.SKIN              -> ItemViewCategoryEnum.INVISIBLE // Different UI
  ItemCategoryEnum.MOBILE_LOOT_BOX   -> ItemViewCategoryEnum.SPECIAL
}
