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

package jp.assasans.narukami.server.lobby.user

import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.time.Duration.Companion.days
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.NodeBuilder
import jp.assasans.narukami.server.core.internal.IDataUnit
import jp.assasans.narukami.server.lobby.UserNode
import jp.assasans.narukami.server.lobby.UsernameComponent
import jp.assasans.narukami.server.net.NettySocketClient
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.command.ProtocolClass
import jp.assasans.narukami.server.net.sessionNotNull

@ProtocolClass(4)
data class UserTemplate(
  val userProperties: IModelProvider<UserPropertiesModelCC>,
  val userNotifier: IModelProvider<UserNotifierModelCC>,
  val uidNotifier: IModelProvider<UidNotifierModelCC>,
  val rankNotifier: IModelProvider<RankNotifierModelCC>,
  val proBattleNotifier: IModelProvider<ProBattleNotifierModelCC>,
) : ITemplate {
  companion object {
    val Provider = ITemplateProvider {
      UserTemplate(
        userProperties = ClosureModelProvider {
          val user = it.adapt<UserNode>(this)
          val ip = (requireSpaceChannel.sessionNotNull.controlChannel.socket as NettySocketClient).channel.remoteAddress()
          UserPropertiesModelCC(
            canUseGroup = false,
            crystals = user.crystals.crystals,
            crystalsRating = 0,
            daysFromLastVisit = 0,
            daysFromRegistration = 0,
            gearScore = 0,
            goldsTakenRating = 0,
            hasSpectatorPermissions = false,
            id = 30,
            rank = 1,
            rankBounds = RankBounds(lowBound = user.score.score / 2, topBound = user.score.score * 2),
            registrationTimestamp = 10,
            score = user.score.score,
            scoreRating = 10,
            uid = "${user.username.username} ($ip)",
            userProfileUrl = "",
            userRating = 0
          )
        },
        userNotifier = ClosureModelProvider {
          UserNotifierModelCC(currentUserId = it.id)
        },
        uidNotifier = ClosureModelProvider {
          UidNotifierModelCC(uid = it.adaptSingle<UsernameComponent>().username, userId = it.id)
        },
        rankNotifier = ClosureModelProvider {
          RankNotifierModelCC(rank = 1, userId = it.id)
        },
        proBattleNotifier = ClosureModelProvider {
          ProBattleNotifierModelCC(abonementRemainingTimeInSec = 2112.days.inWholeSeconds.toInt())
        },
      )
    }
  }
}

fun <T : Node> IGameObject.adapt(context: IModelContext, clazz: KClass<T>): T {
  val builder = NodeBuilder()
  val definition = builder.getNodeDefinition(clazz.createType())
  val node = builder.tryBuildLazy(
    definition,
    models.mapValues { (_, model) ->
      { model.provide(this, context) }
    },
    components
  ) ?: throw IllegalStateException("Failed to build node $clazz")

  return node as T
}

inline fun <reified T : Node> IGameObject.adapt(context: IModelContext): T {
  return adapt(context, T::class)
}

inline fun <reified T : Node> IGameObject.adapt(sender: SpaceChannel): T {
  return adapt(SpaceChannelModelContext(sender), T::class)
}

inline fun <reified T : IDataUnit> IGameObject.adaptSingle(context: IModelContext): T {
  return if(T::class.isSubclassOf(IModelConstructor::class)) {
    @Suppress("UNCHECKED_CAST")
    val provider = requireNotNull(models[T::class as KClass<out IModelConstructor>]) { "No ${T::class} in $this" }
    provider.provide(this, context) as T
  } else if(T::class.isSubclassOf(IComponent::class)) {
    @Suppress("UNCHECKED_CAST")
    requireNotNull(components[T::class as KClass<out IComponent>]) { "No ${T::class} in $this" } as T
  } else {
    throw IllegalArgumentException("Class ${T::class} is not a subclass of IModelConstructor or IComponent")
  }
}

context(SpaceChannel)
inline fun <reified T : IDataUnit> IGameObject.adaptSingle(): T {
  return adaptSingle(SpaceChannelModelContext(this@SpaceChannel))
}

context(IModelContext)
inline fun <reified T : IDataUnit> IGameObject.adaptSingle(): T {
  return adaptSingle(this@IModelContext)
}
