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

package jp.assasans.narukami.server.battlefield.tank

import jp.assasans.narukami.server.net.command.IProtocolEnum
import jp.assasans.narukami.server.net.command.ProtocolEnum

@ProtocolEnum
enum class DamageType(override val value: Int) : IProtocolEnum<Int> {
  SMOKY(0),
  SMOKY_CRITICAL(1),
  FIREBIRD(2),
  FIREBIRD_OVERHEAT(3),
  TWINS(4),
  RAILGUN(5),
  ISIS(6),
  MINE(7),
  THUNDER(8),
  RICOCHET(9),
  FREEZE(10),
  SHAFT(11),
  MACHINE_GUN(12),
  SHOTGUN(13),
  ROCKET(14),
  ARTILLERY(15),
  TERMINATOR(16),
  BOMB(17),
  AT_FIELD(18),
  NUCLEAR(19),
  GAUSS(20),
  SUICIDE(2112),
}
