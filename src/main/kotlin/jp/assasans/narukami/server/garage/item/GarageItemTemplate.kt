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

package jp.assasans.narukami.server.garage.item

import kotlin.time.Duration.Companion.days
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.PersistentTemplateV2
import jp.assasans.narukami.server.core.addModel
import jp.assasans.narukami.server.garage.ItemCategoryEnum
import jp.assasans.narukami.server.garage.ItemViewCategoryEnum
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

object GarageItemTemplate : PersistentTemplateV2(), KoinComponent {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  override fun instantiate(id: Long) = gameObject(id).apply {
    addModel(
      DescriptionModelCC(
        name = "GarageItemTemplate",
        description = "Unimplemented"
      )
    )
    addModel(
      ItemModelCC(
        minRank = 1,
        maxRank = 31,
        position = 0,
        preview = gameResourceRepository.get(
          "tank.hull.viking.preview",
          mapOf("gen" to "1.0", "modification" to "0"),
          ImageRes,
          Lazy
        ),
      )
    )
    addModel(ItemCategoryModelCC(category = ItemCategoryEnum.ARMOR))
    addModel(ItemViewCategoryModelCC(category = ItemViewCategoryEnum.ARMOR))
    addModel(
      BuyableModelCC(
        buyable = true,
        priceWithoutDiscount = 2112,
      )
    )
    addModel(DiscountCollectorModelCC())
    addModel(
      DiscountModelCC(
        discount = 98f,
        timeLeftInSeconds = 1.days.inWholeSeconds.toInt(),
        timeToStartInSeconds = 0,
      )
    )
    addModel(
      TimePeriodModelCC(
        isEnabled = true,
        isTimeless = true,
        timeLeftInSeconds = 0,
        timeToStartInSeconds = 0,
      )
    )
    addModel(PremiumItemModelCC(premiumItem = false))
    addModel(DataOwnerModelCC(dataOwnerId = 0))
  }
}
