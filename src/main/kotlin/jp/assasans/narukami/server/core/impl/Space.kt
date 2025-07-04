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

package jp.assasans.narukami.server.core.impl

import java.util.concurrent.atomic.AtomicLong
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.core.*

class Space(override val id: Long) : ISpace, KoinComponent {
  private val logger = KotlinLogging.logger { }

  companion object {
    private val lastId = AtomicLong(-2112)

    /**
     * Generates a new transient ID for a space.
     *
     * Transient IDs are negative, as opposed to stable IDs, which are positive.
     */
    fun freeId(): Long {
      return lastId.getAndDecrement()
    }

    /**
     * Generates a stable ID for a space.
     *
     * Stable IDs are positive and always the same for the same identifier.
     */
    fun stableId(identifier: String): Long = makeStablePersistentId("Space:$identifier")
  }

  private val eventScheduler: IEventScheduler by inject()

  override val objects: IRegistry<IGameObject> = GameObjectRegistry(this)

  override val rootObject: IGameObject
    get() = objects.get(id) ?: error("No root object for space $id")

  init {
    replaceRootObject(RootTemplate.create(id))
    check(rootObject.parent.id == 0L) { "Root object must have parent with ID 0, but has ${rootObject.parent.id}" }

    eventScheduler.schedule(SpaceCreatedEvent(), SpaceModelContext(this), rootObject)
  }
}
