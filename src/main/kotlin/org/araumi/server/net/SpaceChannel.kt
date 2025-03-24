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

package org.araumi.server.net

import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBufAllocator
import org.araumi.server.dispatcher.GameObject
import org.araumi.server.dispatcher.ModelData
import org.araumi.server.dispatcher.ObjectsData
import org.araumi.server.dispatcher.ObjectsDependencies
import org.araumi.server.entrance.CaptchaModelCC
import org.araumi.server.entrance.EntranceModelCC
import org.araumi.server.entrance.LoginModelCC
import org.araumi.server.extensions.kotlinClass
import org.araumi.server.net.command.*
import org.araumi.server.protocol.ICodec
import org.araumi.server.protocol.OptionalMap
import org.araumi.server.protocol.ProtocolBuffer
import org.araumi.server.protocol.getTypedCodec

@ProtocolStruct
interface IModel

/**
 * A base interface for all events.
 *
 * Note: Event fields are encoded in the order of their declaration.
 */
@ProtocolStruct
@ProtocolPreserveOrder
interface IEvent

/**
 * A server-to-client event (S2C).
 */
interface IClientEvent : IEvent

/**
 * A client-to-server event (C2S).
 */
interface IServerEvent : IEvent

@ProtocolEvent(3216143066888387731)
data class DispatcherModelLoadDependenciesEvent(
  val dependencies: ObjectsDependencies
) : IClientEvent

@ProtocolEvent(7640916300855664666)
data class DispatcherModelLoadObjectsDataEvent(
  val objectsData: ObjectsData
) : IClientEvent

@ProtocolEvent(1816792453857564692)
data class DispatcherModelDependenciesLoadedEvent(
  val callbackId: Int
) : IServerEvent

@ProtocolEvent(108605496059850042)
data class LoginModelLoginEvent(
  val uidOrEmail: String,
  val password: String,
  val remember: Boolean
) : IServerEvent

@get:JvmName("KClass_IEvent_protocolId")
val KClass<out IEvent>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolEvent>()) { "$this has no @ProtocolEvent annotation" }.id

fun IClientEvent.attachTo(objectId: Long) = SpaceCommand(
  header = SpaceCommandHeader(objectId = objectId, methodId = this::class.protocolId),
  body = this
)

@get:JvmName("KClass_IModel_protocolId")
val KClass<out IModel>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolModel>()) { "$this has no @ProtocolModel annotation" }.id

interface IClass

@get:JvmName("KClass_IClass_protocolId")
val KClass<out IClass>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolClass>()) { "$this has no @ProtocolClass annotation" }.id

val KClass<out IClass>.models: List<KClass<out IModel>>
  get() {
    return memberProperties.map { it.returnType.kotlinClass }.filter { it.isSubclassOf(IModel::class) } as List<KClass<out IModel>>
  }

@ProtocolClass(2)
data class EntranceClass(
  val entrance: EntranceModelCC,
  val captcha: CaptchaModelCC,
  val login: LoginModelCC,
) : IClass

interface IGameClass {
  val id: Long
  val models: List<Long>
}

data class GameClass(
  override val id: Long,
  override val models: List<Long>,
) : IGameClass

interface IGameObject {
  val id: Long
  val parent: IGameClass
  val models: Map<KClass<out IModel>, IModel>
}

class TransientGameObject(
  override val id: Long,
  override val parent: IGameClass
) : IGameObject {
  override val models: Map<KClass<out IModel>, IModel> = mutableMapOf()
}

/**
 * All spaces have a Dispatcher object with ID same as the space ID and class `0`.
 *
 * Spaces are used for the actual client-server communication.
 */
class SpaceChannel(socket: ISocketClient) : ChannelKind(socket) {
  private val logger = KotlinLogging.logger { }

  private val commandHeaderCodec = protocol.getTypedCodec<SpaceCommandHeader>()

  init {
    logger.info { "EntranceClass models: ${EntranceClass::class.models}" }

    sendBatched {
      DispatcherModelLoadDependenciesEvent(
        dependencies = ObjectsDependencies(
          callbackId = 2,
          classes = listOf(
            GameClass(2, EntranceClass::class.models.map { it.protocolId })
          ),
          resources = listOf()
        )
      ).attachTo(0xaa55).enqueue()

      DispatcherModelLoadObjectsDataEvent(
        objectsData = ObjectsData(
          objects = listOf(
            GameObject(1, EntranceClass::class.protocolId)
          ),
          modelData = listOf(
            ModelData.newObject(1),
            ModelData.newModel(EntranceModelCC::class.protocolId, EntranceModelCC(antiAddictionEnabled = false)),
            ModelData.newModel(CaptchaModelCC::class.protocolId, CaptchaModelCC(stateWithCaptcha = listOf()))
          )
        )
      ).attachTo(0xaa55).enqueue()
    }
  }

  override fun process(buffer: ProtocolBuffer) {
    var commandIndex = 0
    while(buffer.data.readableBytes() > 0) {
      logger.trace { "Processing command #$commandIndex" }
      commandIndex++

      val command = commandHeaderCodec.decode(buffer)
      logger.trace { "Received $command" }

      val serverEvents = mutableMapOf<Long, KClass<out IServerEvent>>()
      ClassGraph().enableAllInfo().acceptPackages("org.araumi").scan().use { scanResult ->
        val classes = scanResult.getClassesImplementing(IServerEvent::class.qualifiedName).loadClasses().map { it.kotlin } as List<KClass<out IServerEvent>>
        logger.info { "Found ${classes.size} server events" }

        for(clazz in classes) {
          logger.debug { "Discovered server event: $clazz" }
          serverEvents[clazz.protocolId] = clazz
        }
      }

      val eventClass = serverEvents[command.methodId]
      if(eventClass == null) {
        logger.error { "Unknown method: $command" }
        return
      }

      val codec = protocol.getCodec(eventClass.createType()) as ICodec<IEvent>
      val event = codec.decode(buffer)
      logger.info { "Received server event: $event" }
    }

    if(buffer.data.readableBytes() > 0) {
      logger.warn { "Buffer has ${buffer.data.readableBytes()} bytes left" }
    }
  }

  fun sendBatched(block: SpaceChannelOutgoingBatch.() -> Unit) {
    val batch = SpaceChannelOutgoingBatch()
    block(batch)

    val buffer = ProtocolBuffer(ByteBufAllocator.DEFAULT.buffer(), OptionalMap())

    logger.trace { "Encoding batch with ${batch.commands.size} commands" }
    for(command in batch.commands) {
      logger.trace { "Encoding $command" }

      commandHeaderCodec.encode(buffer, command.header)

      val bodyCodec = protocol.getCodec(command.body::class.createType()) as ICodec<Any>
      bodyCodec.encode(buffer, command.body)
    }

    logger.trace { "Sending batch with ${batch.commands.size} commands" }
    socket.send(buffer)
  }
}

class SpaceChannelOutgoingBatch {
  val commands: MutableList<SpaceCommand> = mutableListOf()

  fun SpaceCommand.enqueue() {
    commands.add(this)
  }
}
