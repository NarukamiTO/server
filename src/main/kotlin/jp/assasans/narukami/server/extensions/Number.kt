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

package jp.assasans.narukami.server.extensions

fun Int.roundToNearest(nearest: Int): Int {
  return ((this + nearest / 2) / nearest) * nearest
}

/**
 * Constant by which to multiply an angular value in degrees to obtain an
 * angular value in radians.
 */
private const val DEGREES_TO_RADIANS = 0.017453292f

/**
 * Constant by which to multiply an angular value in radians to obtain an
 * angular value in degrees.
 */
private const val RADIANS_TO_DEGREES = 57.29578f

fun Float.toRadians(): Float = this * DEGREES_TO_RADIANS
fun Float.toDegrees(): Float = this * RADIANS_TO_DEGREES
