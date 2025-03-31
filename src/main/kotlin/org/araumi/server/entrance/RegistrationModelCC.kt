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

package org.araumi.server.entrance

import org.araumi.server.core.IModelConstructor
import org.araumi.server.net.command.ProtocolModel
import org.araumi.server.res.ImageRes
import org.araumi.server.res.Resource

@ProtocolModel(2474458842977623992)
data class RegistrationModelCC(
  val bgResource: Resource<ImageRes, *>,
  val enableRequiredEmail: Boolean,
  val maxPasswordLength: Int,
  val minPasswordLength: Int,
) : IModelConstructor {
  override fun getResources(): List<Resource<*, *>> {
    return listOf(bgResource)
  }
}
