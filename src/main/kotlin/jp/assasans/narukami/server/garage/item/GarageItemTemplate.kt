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
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.core.ITemplateProvider
import jp.assasans.narukami.server.garage.ItemCategoryEnum
import jp.assasans.narukami.server.garage.ItemViewCategoryEnum
import jp.assasans.narukami.server.net.command.ProtocolClass
import jp.assasans.narukami.server.res.ImageRes
import jp.assasans.narukami.server.res.Lazy
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

@ProtocolClass(8537)
data class GarageItemTemplate(
  val description: DescriptionModelCC,
  val item: ItemModelCC,
  val itemCategory: ItemCategoryModelCC,
  val itemViewCategory: ItemViewCategoryModelCC,
  val buyableModelCC: BuyableModelCC,
  val discountCollector: DiscountCollectorModelCC,
  val discountModel: DiscountModelCC,
  val timePeriod: TimePeriodModelCC,
  val premiumItem: PremiumItemModelCC,
  val dataOwner: DataOwnerModelCC,
) : ITemplate {
  companion object : KoinComponent {
    private val gameResourceRepository: RemoteGameResourceRepository by inject()

    val Provider = ITemplateProvider {
      GarageItemTemplate(
        description = DescriptionModelCC(
          name = "GarageItemTemplate",
          description = "Unimplemented"
        ),
        item = ItemModelCC(
          minRank = 1,
          maxRank = 31,
          position = 0,
          preview = gameResourceRepository.get(
            "tank.hull.viking.preview",
            mapOf("gen" to "1.0", "modification" to "0"),
            ImageRes,
            Lazy
          ),
        ),
        itemCategory = ItemCategoryModelCC(category = ItemCategoryEnum.ARMOR),
        itemViewCategory = ItemViewCategoryModelCC(category = ItemViewCategoryEnum.ARMOR),
        buyableModelCC = BuyableModelCC(
          buyable = true,
          priceWithoutDiscount = 2112,
        ),
        discountCollector = DiscountCollectorModelCC(),
        discountModel = DiscountModelCC(
          discount = 98f,
          timeLeftInSeconds = 1.days.inWholeSeconds.toInt(),
          timeToStartInSeconds = 0,
        ),
        timePeriod = TimePeriodModelCC(
          isEnabled = true,
          isTimeless = true,
          timeLeftInSeconds = 0,
          timeToStartInSeconds = 0,
        ),
        premiumItem = PremiumItemModelCC(premiumItem = false),
        dataOwner = DataOwnerModelCC(dataOwnerId = 0)
      )
    }
  }
}
