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

package jp.assasans.narukami.server.entrance

import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.UserGroupComponent
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.dispatcher.DispatcherOpenSpaceEvent
import jp.assasans.narukami.server.dispatcher.preloadResources
import jp.assasans.narukami.server.extensions.roundToNearest
import jp.assasans.narukami.server.lobby.CrystalsComponent
import jp.assasans.narukami.server.lobby.ScoreComponent
import jp.assasans.narukami.server.lobby.UsernameComponent
import jp.assasans.narukami.server.lobby.user.UserTemplate
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.LocalizedImageRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository

data class EntranceNode(
  val entrance: EntranceModelCC,
) : Node()

class KClassSerializer : JsonSerializer<KClass<*>>() {
  override fun serialize(value: KClass<*>, generator: JsonGenerator, serializers: SerializerProvider) {
    generator.writeString(value.jvmName)
  }
}

class KClassKeySerializer : JsonSerializer<KClass<*>>() {
  override fun serialize(value: KClass<*>, generator: JsonGenerator, serializers: SerializerProvider) {
    generator.writeFieldName(value.jvmName)
  }
}

class ComponentMapSerializer : JsonSerializer<Map<KClass<out IComponent>, IComponent>>() {
  override fun serialize(value: Map<KClass<out IComponent>, IComponent>, generator: JsonGenerator, serializers: SerializerProvider) {
    generator.writeStartObject()
    for((key, component) in value) {
      generator.writeObjectField(key.jvmName, component)
    }
    generator.writeEndObject()
  }
}

class ComponentMapDeserializer : JsonDeserializer<Map<KClass<out IComponent>, IComponent>>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): Map<KClass<out IComponent>, IComponent> {
    val components = mutableMapOf<KClass<out IComponent>, IComponent>()

    val mapper = parser.codec as ObjectMapper
    val node = mapper.readTree<ObjectNode>(parser)
    for((key, value) in node.fields()) {
      val clazz = try {
        Class.forName(key).kotlin
      } catch(exception: ClassNotFoundException) {
        throw IllegalArgumentException("Class '$key' not found", exception)
      }
      if(!clazz.isSubclassOf(IComponent::class)) {
        throw IllegalArgumentException("Class '$key' is not a component")
      }
      @Suppress("UNCHECKED_CAST")
      clazz as KClass<out IComponent>

      val jacksonType = context.typeFactory.constructType(clazz.java)
      val deserializer = context.findRootValueDeserializer(jacksonType)
      val nodeParser = value.traverse(context.parser.codec)
      nodeParser.nextToken()

      val component = deserializer.deserialize(nodeParser, context) as IComponent
      components[clazz] = component
    }

    return components
  }
}

class TemplateV2Serializer : JsonSerializer<TemplateV2>() {
  override fun serialize(value: TemplateV2, generator: JsonGenerator, serializers: SerializerProvider) {
    generator.writeString(value::class.jvmName)
  }
}

class TemplateV2Deserializer : JsonDeserializer<TemplateV2>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): TemplateV2 {
    val className = parser.valueAsString
    try {
      val clazz = Class.forName(className).kotlin
      if(!clazz.isSubclassOf(TemplateV2::class)) {
        throw IllegalArgumentException("Class '$className' is not a TemplateV2")
      }
      @Suppress("UNCHECKED_CAST")
      clazz as KClass<out TemplateV2>

      return clazz.objectInstance ?: throw IllegalArgumentException("Class '$className' is not an object declaration")
    } catch(exception: ClassNotFoundException) {
      throw IllegalArgumentException("Class '$className' not found", exception)
    }
  }
}

data class ExternGameObject(
  val id: Long,
  val template: PersistentTemplateV2,
  @field:JsonSerialize(using = ComponentMapSerializer::class)
  val components: Map<KClass<out IComponent>, IComponent>,
)

class LoginSystem : AbstractSystem(), KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun login(
    event: LoginModelLoginEvent,
    entrance: EntranceNode,
    @JoinAll dispatcher: DispatcherNode,
  ) {
    logger.info { "Login event: $event" }

    if(event.password.isNotEmpty() && event.password.length % 2 == 0) {
      LoginModelWrongPasswordEvent().schedule(entrance)
      EntranceAlertModelShowAlertEvent(
        image = gameResourceRepository.get("alert.restrict", emptyMap(), LocalizedImageRes, Eager),
        header = "Login failed",
        text = "Wrong password"
      ).preloadResources().schedule(entrance)
      return
    }

    // Create a user object and assign it to the session.
    // The user object is destroyed immediately when the session is closed,
    // without a grace period like in battles.
    val id = makeStablePersistentId("GameObject:User:${event.uidOrEmail}")
    val transientId = makeStableTransientId("GameObject:User:${event.uidOrEmail}:${Clock.System.now().toEpochMilliseconds()}")
    // XXX: We need to have a transient ID for the user object because game developers are [stupid].
    //  They require the tank object to have an ID of user, and considering that we cannot have two
    //  objects with the same ID in the same space, this makes it impossible to have a user object
    //  and a tank object in the battle space. This behavior deviates from the official server, which
    //  somehow uses the same ID for both user and tank objects. A possible solution is to have
    //  a separate "client ID" for game objects.
    val userObject = UserTemplate.instantiate(transientId).apply {
      addComponent(UserGroupComponent(id))
      addComponent(UsernameComponent(event.uidOrEmail))
      addComponent(ScoreComponent(Random.nextInt(10_000, 1_000_000).roundToNearest(100)))
      addComponent(CrystalsComponent(Random.nextInt(100_000, 10_000_000).roundToNearest(100)))
    }
    entrance.context.requireSpaceChannel.sessionNotNull.user = userObject

    val channel = DispatcherOpenSpaceEvent(Space.stableId("lobby")).schedule(dispatcher).await()
    channel.space.objects.add(userObject)

    // Close the entrance space channel to trigger loading screen on the client
    entrance.context.requireSpaceChannel.close()
  }
}
