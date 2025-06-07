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

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.PersistentTemplateV2
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.Object3DRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository
import jp.assasans.narukami.server.res.TextureRes

object GarageTemplate : PersistentTemplateV2(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  override fun instantiate(id: Long) = gameObject(id).apply {
    // Commented out are old garage parameters, old garage requires "ext-garage-holiday" client extension
    addModel(
      GarageModelCC(
        cameraAltitude = -250f,
        // cameraAltitude = 0f,
        cameraDistance = -950f,
        // cameraDistance = -730f,
        cameraFov = 1.7453293f,
        // cameraFov = 1.5707964f,
        cameraPitch = 260f,
        // cameraPitch = -135f,
        garageBox = gameResourceRepository.get("garage.box", mapOf("gen" to "2.1"), Object3DRes, Eager),
        hideLinks = false,
        mountableCategories = listOf(
          ItemCategoryEnum.ARMOR,
          ItemCategoryEnum.WEAPON,
          ItemCategoryEnum.PAINT
        ),
        skyboxBackSide = gameResourceRepository.get("skybox.mountains.back", mapOf("gen" to "1.0"), TextureRes, Eager),
        skyboxBottomSide = gameResourceRepository.get("skybox.mountains.bottom", mapOf("gen" to "1.0"), TextureRes, Eager),
        skyboxFrontSide = gameResourceRepository.get("skybox.mountains.front", mapOf("gen" to "1.0"), TextureRes, Eager),
        skyboxLeftSide = gameResourceRepository.get("skybox.mountains.left", mapOf("gen" to "1.0"), TextureRes, Eager),
        skyboxRightSide = gameResourceRepository.get("skybox.mountains.right", mapOf("gen" to "1.0"), TextureRes, Eager),
        skyboxTopSide = gameResourceRepository.get("skybox.mountains.top", mapOf("gen" to "1.0"), TextureRes, Eager)
      )
    )
    addModel(UpgradeGarageItemModelCC())
    addModel(PassToShopModelCC(passToShopEnabled = false))
  }
}
