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

package jp.assasans.narukami.server.battlefield

import java.nio.file.Paths
import kotlin.io.path.readText
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.koin.core.component.inject
import jp.assasans.narukami.server.battleselect.PrivateMapDataEntity
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherLoadDependenciesManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherLoadObjectsManagedEvent
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.MapRes
import jp.assasans.narukami.server.res.ProplibRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

class BattlefieldSystem : AbstractSystem() {
  private val gameResourceRepository: RemoteGameResourceRepository by inject()
  private val objectMapper: ObjectMapper by inject()

  @OnEventFire
  @OutOfOrderExecution
  suspend fun channelAdded(
    event: ChannelAddedEvent,
    dispatcher: DispatcherNode,
    @JoinAll battleMap: SingleNode<BattleMapModelCC>,
    @JoinAll battlefield: SingleNode<BattlefieldModelCC>,
  ) {
    val mapResource = gameResourceRepository.get(
      "map.spawn-test",
      mapOf(
        "gen" to "2.1",
        "variant" to "default",
      ),
      MapRes,
      Eager
    )

    // TODO: Temporary solution
    val root = Paths.get(requireNotNull(System.getenv("RESOURCES_ROOT")) { "\"RESOURCES_ROOT\" environment variable is not set" })
    val text = root.resolve("${mapResource.id.encode()}/private.json").readText()
    val data = objectMapper.readValue<PrivateMapDataEntity>(text)

    DispatcherLoadDependenciesManagedEvent(
      classes = emptyList(),
      resources = data.proplibs.map {
        gameResourceRepository.get(
          it.name,
          mapOf(
            "gen" to "1.0-redefined",
            "theme" to "summer"
          ),
          ProplibRes,
          Eager
        )
      }
    ).schedule(dispatcher).await()

    DispatcherLoadObjectsManagedEvent(
      listOf(
        battleMap.gameObject,
        battlefield.gameObject,
      )
    ).schedule(dispatcher).await()
  }
}
