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

package jp.assasans.narukami.server.core

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import jp.assasans.narukami.server.core.impl.EventScheduler
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.command.ProtocolEvent
import jp.assasans.narukami.server.net.command.ProtocolPreserveOrder
import jp.assasans.narukami.server.net.command.ProtocolStruct

/**
 * A base interface for all events.
 *
 * Note: Event fields are encoded in the order of their declaration.
 */
@ProtocolStruct
@ProtocolPreserveOrder
interface IEvent

@get:JvmName("KClass_IEvent_protocolId")
val KClass<out IEvent>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolEvent>()) { "$this has no @ProtocolEvent annotation" }.id

context(AbstractSystem)
fun <T : IEvent> T.schedule(node: Node): T {
  eventScheduler.schedule(this, node.sender, node.gameObject)
  return this
}

context(AbstractSystem)
fun <T : IEvent> T.schedule(sender: SpaceChannel, gameObject: IGameObject): T {
  eventScheduler.schedule(this, sender, gameObject)
  return this
}

context(SpaceChannel)
fun <T : IEvent> T.schedule(gameObject: IGameObject): T {
  eventScheduler.schedule(this, this@SpaceChannel, gameObject)
  return this
}

fun <T : IEvent> T.schedule(scheduler: EventScheduler, sender: SpaceChannel, gameObject: IGameObject): T {
  scheduler.schedule(this, sender, gameObject)
  return this
}
