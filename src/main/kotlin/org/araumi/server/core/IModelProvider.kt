/*
 * Araumi TO - a server software reimplementation for a certain browser tank game.
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

package org.araumi.server.core

import org.araumi.server.net.SpaceChannel

/**
 * Provides a model constructor for a given space channel (usually for a given session).
 *
 * @see IModelConstructor
 */
interface IModelProvider<CC : IModelConstructor> {
  /**
   * Provides a model constructor for the given space channel.
   */
  fun provide(gameObject: IGameObject, channel: SpaceChannel): CC
}
