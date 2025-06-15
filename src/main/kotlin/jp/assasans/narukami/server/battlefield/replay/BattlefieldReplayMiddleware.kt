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

package jp.assasans.narukami.server.battlefield.replay

import kotlin.reflect.KClass
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import jp.assasans.narukami.server.battlefield.BattlefieldModelCC
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.entrance.ComponentMapDeserializer
import jp.assasans.narukami.server.entrance.TemplateV2Deserializer
import jp.assasans.narukami.server.extensions.hasInheritedAnnotation
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.session.SessionHash

// TODO: All code in this file is pretty bad and should be refactored

const val IS_RECORDING = false

object BattlefieldReplayMiddleware : EventMiddleware {
  private val logger = KotlinLogging.logger { }

  var replayWriter: ReplayWriter? = null

  override fun process(eventScheduler: IEventScheduler, event: IEvent, gameObject: IGameObject, context: IModelContext) {
    if(!context.space.rootObject.models.contains(BattlefieldModelCC::class)) return

    if(IS_RECORDING) {
      if(replayWriter == null) {
        replayWriter = ReplayWriter(context.space)
        replayWriter!!.writeComment("replay started at ${Clock.System.now()}")
        replayWriter!!.writeComment("space id: ${context.space.id}")
        logger.info { "Replay writer initialized for space ${context.space.id}" }
      }

      if(event !is IServerEvent && !event::class.hasInheritedAnnotation<ReplayRecord>()) return
      if(context !is SpaceChannelModelContext) return

      replayWriter!!.writeEvent(event, gameObject, context.channel)

      logger.debug { "Recorded: ${event::class.simpleName}" }
    }

    // eventScheduler.schedule(BattleDebugMessageEvent("${event::class.simpleName}"), context, context.space.rootObject)
  }
}

sealed interface IReplayEntry {
  val timestamp: Long
}

data class ReplayExternObject(
  override val timestamp: Long,
  val gameObject: IGameObject,
) : IReplayEntry

data class ReplayUser(
  override val timestamp: Long,
  val session: SessionHash,
  val userObject: IGameObject,
) : IReplayEntry

data class ReplayEvent(
  override val timestamp: Long,
  val sender: SpaceChannel,
  val gameObject: IGameObject,
  val event: IEvent,
) : IReplayEntry

fun deserializeExternObject(objectMapper: ObjectMapper, src: String, registry: IRegistry<IGameObject>, isolate: Boolean): IGameObject {
  val logger = KotlinLogging.logger { }

  val parser = objectMapper.createParser(src)
  val mapper = parser.codec as ObjectMapper
  // TODO: What the hell is this? Jackson's API is so convoluted...
  //  You cannot call deserializer manually without some cursed code.
  val context = (mapper.deserializationContext as DefaultDeserializationContext).createInstance(
    mapper.deserializationConfig,
    parser,
    InjectableValues.Std()
  )

  val node = mapper.readTree<ObjectNode>(parser)

  val id = node.get("id")?.asLong() ?: throw IllegalArgumentException("Missing 'id' field")
  val templateNode = node.get("template") ?: throw IllegalArgumentException("Missing 'template' field")
  val template = run {
    val deserializer = TemplateV2Deserializer()
    val nodeParser = templateNode.traverse(context.parser.codec)
    nodeParser.nextToken()

    deserializer.deserialize(nodeParser, context) as PersistentTemplateV2
  }

  val isolatedId = if(isolate) isolateId(id) else id
  logger.info { "Isolated ID: $id -> $isolatedId" }
  val gameObject = template.instantiate(isolatedId)
  val wrappedRegistry = FakeGameObjectRegistry(registry, gameObject)
  context.setAttribute("gameObjectRegistry", wrappedRegistry)

  val components = run {
    val componentsNode = node.get("components") ?: throw IllegalArgumentException("Missing 'components' field")
    val deserializer = ComponentMapDeserializer()
    val nodeParser = componentsNode.traverse(context.parser.codec)
    nodeParser.nextToken()

    deserializer.deserialize(nodeParser, context) as Map<KClass<out IComponent>, IComponent>
  }
  gameObject.addAllComponents(components.values)

  return gameObject
}
