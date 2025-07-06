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

package jp.assasans.narukami.server.battlefield

import kotlin.reflect.full.hasAnnotation
import jp.assasans.narukami.server.battlefield.chat.BattleDebugMessageEvent
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.EventHandlerV2Definition
import jp.assasans.narukami.server.lobby.UsernameComponent
import jp.assasans.narukami.server.lobby.communication.remote
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BattlefieldLogEvent

object BattlefieldDebugMiddleware : EventMiddleware {
  override fun processHandler(eventScheduler: IEventScheduler, event: IEvent, gameObject: IGameObject, context: IModelContext, handler: EventHandlerV2Definition) {
    if(!handler.function.hasAnnotation<BattlefieldLogEvent>()) return

    val username = context.requireSpaceChannel.sessionNotNull.userNotNull.getComponent<UsernameComponent>().username
    val battlefieldObject = context.space.rootObject
    val contexts = with(context) { remote(battlefieldObject) }
    for(context in contexts) {
      eventScheduler.schedule(BattleDebugMessageEvent("$username: $event"), context, battlefieldObject)
    }
  }
}
