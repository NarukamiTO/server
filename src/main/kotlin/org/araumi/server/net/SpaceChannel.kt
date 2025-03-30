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
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBufAllocator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.araumi.server.dispatcher.ModelData
import org.araumi.server.dispatcher.ObjectsData
import org.araumi.server.dispatcher.ObjectsDependencies
import org.araumi.server.dispatcher.ResourceDependency
import org.araumi.server.entrance.*
import org.araumi.server.extensions.kotlinClass
import org.araumi.server.net.command.*
import org.araumi.server.protocol.ICodec
import org.araumi.server.protocol.OptionalMap
import org.araumi.server.protocol.ProtocolBuffer
import org.araumi.server.protocol.getTypedCodec
import org.araumi.server.res.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IModel

@ProtocolStruct
interface IModelConstructor {
  fun getResources(): List<Resource<*, *>> {
    return emptyList()
  }
}

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

@ProtocolEvent(6160917066910786241)
class LoginModelWrongPasswordEvent : IClientEvent

@ProtocolEvent(7216954482225034551)
class EntranceAlertModelShowAlertEvent(
  val image: Resource<LocalizedImageRes, Eager>,
  val header: String,
  val text: String
) : IClientEvent

data class DispatcherModelLoadDependenciesManagedEvent(
  val classes: List<IGameClass>,
  val resources: List<Resource<*, *>>,
) : IEvent {
  private val logger = KotlinLogging.logger { }

  var callbackId: Int = 0
  val deferred: CompletableDeferred<Unit> = CompletableDeferred()

  suspend fun await() {
    logger.info { "Waiting for dependencies $callbackId to load..." }
    return deferred.await()
  }
}

data class PreloadResourcesWrappedEvent<T : IEvent>(
  val event: T
) : IEvent

@get:JvmName("KClass_IEvent_protocolId")
val KClass<out IEvent>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolEvent>()) { "$this has no @ProtocolEvent annotation" }.id

fun <T : IEvent> T.preloadResources(): PreloadResourcesWrappedEvent<T> {
  return PreloadResourcesWrappedEvent(this)
}

context(AbstractSystem)
fun <T : IEvent> T.schedule(node: Node): T {
  eventScheduler.process(this, node.sender, node.gameObject)
  return this
}

context(AbstractSystem)
fun <T : IEvent> T.schedule(sender: SpaceChannel, gameObject: IGameObject<*>): T {
  eventScheduler.process(this, sender, gameObject)
  return this
}

context(SpaceChannel)
fun <T : IEvent> T.schedule(gameObject: IGameObject<*>): T {
  eventScheduler.process(this, this@SpaceChannel, gameObject)
  return this
}

fun <T : IEvent> T.schedule(scheduler: EventScheduler, sender: SpaceChannel, gameObject: IGameObject<*>): T {
  scheduler.process(this, sender, gameObject)
  return this
}

fun IClientEvent.attach(gameObject: IGameObject<*>) = SpaceCommand(
  header = SpaceCommandHeader(objectId = gameObject.id, methodId = this::class.protocolId),
  body = this
)

fun IClientEvent.attach(node: Node) = attach(node.gameObject)

@get:JvmName("KClass_IModel_protocolId")
val KClass<out IModelConstructor>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolModel>()) { "$this has no @ProtocolModel annotation" }.id

interface ITemplate

@get:JvmName("KClass_IClass_protocolId")
val KClass<out ITemplate>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolClass>()) { "$this has no @ProtocolClass annotation" }.id

val KClass<out ITemplate>.models: List<KClass<out IModelConstructor>>
  get() {
    return memberProperties.map { it.returnType.kotlinClass }.filter { it.isSubclassOf(IModelConstructor::class) } as List<KClass<out IModelConstructor>>
  }

@ProtocolClass(2)
data class EntranceTemplate(
  val entrance: EntranceModelCC,
  val captcha: CaptchaModelCC,
  val login: LoginModelCC,
  val registration: RegistrationModelCC,
  val entranceAlert: EntranceAlertModelCC,
) : ITemplate

interface IGameClass {
  val id: Long
  val models: List<Long>
}

data class TransientGameClass(
  override val id: Long,
  override val models: List<Long>,
) : IGameClass {
  companion object {
    // fun fromTemplate(template: KClass<out ITemplate>): TransientGameClass {
    //   return TransientGameClass(
    //     id = template.protocolId,
    //     models = template.models.map { it.protocolId }
    //   )
    // }
  }
}

data class TemplatedGameClass<T : ITemplate>(
  override val id: Long,
  override val models: List<Long>,
  val template: KClass<T>,
) : IGameClass {
  private val logger = KotlinLogging.logger { }

  companion object {
    fun <T : ITemplate> fromTemplate(template: KClass<T>): TemplatedGameClass<T> {
      return TemplatedGameClass(
        id = template.protocolId,
        models = template.models.map { it.protocolId },
        template = template
      )
    }
  }
}

interface IGameObject<TClass : IGameClass> {
  val id: Long
  val parent: TClass
  val models: Map<KClass<out IModelConstructor>, IModelConstructor>
}

class TransientGameObject<TClass : IGameClass>(
  override val id: Long,
  override val parent: TClass
) : IGameObject<TClass> {
  companion object {
    private val logger = KotlinLogging.logger { }

    fun <T : ITemplate> instantiate(id: Long, parent: TemplatedGameClass<T>, template: T): TransientGameObject<TemplatedGameClass<T>> {
      val gameObject = TransientGameObject(id, parent)

      parent.models.forEach { modelId ->
        val modelClass = template::class.models.first { it.protocolId == modelId }
        val model = template::class.memberProperties.first { it.returnType.kotlinClass == modelClass }
          .getter.call(template) as IModelConstructor
        gameObject.models[modelClass] = model

        logger.debug { "Instantiated $model from $template" }
      }

      return gameObject
    }
  }

  override val models: MutableMap<KClass<out IModelConstructor>, IModelConstructor> = mutableMapOf()
}

fun <T : ITemplate> IGameObject<TemplatedGameClass<T>>.adapt(): T {
  val logger = KotlinLogging.logger { }
  logger.debug { "Adapting ${this.models}" }

  val templateClass = parent.template
  val template = templateClass.constructors.first().callBy(
    templateClass.constructors.first().parameters.associateWith { parameter ->
      models[parameter.type.kotlinClass] ?: error("Missing model for parameter $parameter")
    }
  )

  return template
}

abstract class AbstractSystem {
  val eventScheduler: EventScheduler = EventScheduler()
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnEventFire

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Mandatory

@ProtocolModel(-1)
data class DeferredDependenciesCC(
  val callbackId: Int,
  val deferred: CompletableDeferred<Unit>,
) : IModelConstructor

@ProtocolClass(-1)
data class DeferredDependenciesTemplate(
  val deferredDependencies: DeferredDependenciesCC,
) : ITemplate

class DispatcherSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  suspend fun preloadResourcesWrapped(event: PreloadResourcesWrappedEvent<*>, any: Node) {
    // get resources from event properties using reflection
    val resources = event.event::class
      .declaredMemberProperties
      .filter { it.returnType.kotlinClass.isSubclassOf(Resource::class) }
      .map { it.getter.call(event.event) as Resource<*, *> }
      .toList()
    logger.info { "Preload resources: $resources" }

    DispatcherModelLoadDependenciesManagedEvent(
      classes = listOf(),
      resources = resources
    ).schedule(any.sender, any.gameObject).await()
    logger.info { "Resources preloaded" }

    event.event.schedule(any.sender, any.gameObject)
  }

  @OnEventFire
  @Mandatory
  suspend fun loadDependenciesManaged(event: DispatcherModelLoadDependenciesManagedEvent, any: Node) {
    logger.info { "Load dependencies managed: $event" }

    event.callbackId = event.deferred.hashCode()

    val deferredDependenciesClass = TemplatedGameClass.fromTemplate(DeferredDependenciesTemplate::class)
    val deferredDependenciesObject = TransientGameObject.instantiate(
      id = event.callbackId.toLong(),
      deferredDependenciesClass,
      DeferredDependenciesTemplate(
        deferredDependencies = DeferredDependenciesCC(
          callbackId = event.callbackId,
          deferred = event.deferred
        )
      )
    )
    logger.info { "DD object: ${deferredDependenciesObject.models}" }
    logger.info { "DD object: ${deferredDependenciesObject.parent.models}" }
    any.sender.gameObjectRegistry.add(deferredDependenciesObject)

    DispatcherModelLoadDependenciesEvent(
      dependencies = makeObjectsDependencies(
        callbackId = event.callbackId,
        classes = event.classes,
        resources = event.resources
      )
    ).schedule(any.sender, any.gameObject)
  }

  @OnEventFire
  @Mandatory
  suspend fun dependenciesLoaded(event: DispatcherModelDependenciesLoadedEvent, any: Node) {
    logger.info { "Dependencies loaded: ${event.callbackId}" }

    val deferredDependenciesObject = any.sender.gameObjectRegistry.get(event.callbackId.toLong()) as IGameObject<TemplatedGameClass<DeferredDependenciesTemplate>>?
                                     ?: error("Deferred dependencies object ${event.callbackId} not found")
    val (deferredDependencies) = deferredDependenciesObject.adapt()
    deferredDependencies.deferred.complete(Unit)
    any.sender.gameObjectRegistry.remove(deferredDependenciesObject)
    logger.info { "Deferred dependencies $deferredDependencies resolved" }
  }
}

class LoginSystem : AbstractSystem(), KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  @OnEventFire
  @Mandatory
  suspend fun login(event: LoginModelLoginEvent, entrance: EntranceNode) {
    logger.info { "Login event: $event" }
    entrance.sender.sendBatched {
      LoginModelWrongPasswordEvent().attach(entrance).enqueue()
    }

    EntranceAlertModelShowAlertEvent(
      image = gameResourceRepository.get("alert.restrict", emptyMap(), LocalizedImageRes, Eager),
      header = "Login failed",
      text = "Wrong password"
    ).preloadResources().schedule(entrance)
  }
}

open class Node {
  lateinit var sender: SpaceChannel
    private set
  lateinit var gameObject: IGameObject<*>
    private set

  fun init(sender: SpaceChannel, gameObject: IGameObject<*>) {
    this.sender = sender
    this.gameObject = gameObject
  }
}

data class EntranceNode(
  val entrance: EntranceModelCC,
) : Node()

interface IGameObjectRegistry {
  val all: Set<IGameObject<*>>

  fun add(gameObject: IGameObject<*>)
  fun remove(gameObject: IGameObject<*>)
  fun get(id: Long): IGameObject<*>?
  fun has(id: Long): Boolean
}

fun makeObjectsDependencies(callbackId: Int, classes: List<IGameClass>, resources: List<Resource<*, *>>): ObjectsDependencies {
  return ObjectsDependencies(
    callbackId,
    classes = classes,
    resources = resources.map { resource ->
      ResourceDependency(
        id = resource.id.id,
        version = resource.id.version,
        kind = resource.type.type.id,
        lazy = resource.laziness.isLazy,
        dependents = listOf()
      )
    }
  )
}

fun makeObjectsData(objects: List<IGameObject<*>>): ObjectsData {
  return ObjectsData(
    objects = objects,
    modelData = objects.flatMap { gameObject ->
      listOf(
        ModelData.newObject(gameObject.id)
      ) + gameObject.models
        .filter { (clazz, _) -> clazz.declaredMembers.isNotEmpty() }
        .map { (clazz, model) -> ModelData.newModel(clazz.protocolId, model) }
    }
  )
}

class GameObjectRegistry : IGameObjectRegistry {
  private val logger = KotlinLogging.logger { }

  private val gameObjects: MutableMap<Long, IGameObject<*>> = mutableMapOf()

  override val all: Set<IGameObject<*>>
    get() = gameObjects.values.toSet()

  override fun add(gameObject: IGameObject<*>) {
    gameObjects[gameObject.id] = gameObject
  }

  override fun remove(gameObject: IGameObject<*>) {
    gameObjects.remove(gameObject.id)
  }

  override fun get(id: Long): IGameObject<*>? {
    return gameObjects[id]
  }

  override fun has(id: Long): Boolean {
    return gameObjects.contains(id)
  }
}

/**
 * All spaces have a Dispatcher object with ID same as the space ID and class `0`.
 *
 * Spaces are used for the actual client-server communication.
 */
class SpaceChannel(socket: ISocketClient) : ChannelKind(socket), KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  private val commandHeaderCodec = protocol.getTypedCodec<SpaceCommandHeader>()

  val gameObjectRegistry: IGameObjectRegistry = GameObjectRegistry()
  val eventScheduler: EventScheduler = EventScheduler()

  suspend fun init() {
    logger.info { "EntranceClass models: ${EntranceTemplate::class.models}" }

    val entranceClass = TemplatedGameClass.fromTemplate(EntranceTemplate::class)
    val entranceObject = TransientGameObject.instantiate(
      id = 2,
      entranceClass,
      EntranceTemplate(
        entrance = EntranceModelCC(antiAddictionEnabled = false),
        captcha = CaptchaModelCC(stateWithCaptcha = listOf()),
        login = LoginModelCC(),
        registration = RegistrationModelCC(
          bgResource = gameResourceRepository.get("auth.registration.background-agpl", emptyMap(), ImageRes, Eager),
          enableRequiredEmail = false,
          minPasswordLength = 6,
          maxPasswordLength = 20
        ),
        entranceAlert = EntranceAlertModelCC()
      )
    )
    logger.info { entranceObject.adapt() }

    val rootGameClass = TransientGameClass(
      id = 0,
      models = listOf()
    )
    val rootGameObject = TransientGameObject<IGameClass>(id = 0xaa55, parent = rootGameClass)
    gameObjectRegistry.add(rootGameObject)

    val objects = listOf(entranceObject)
    DispatcherModelLoadDependenciesManagedEvent(
      classes = objects.map { it.parent },
      resources = objects.flatMap { gameObject ->
        gameObject.models.values.flatMap { model ->
          model.getResources()
        }
      }
    ).schedule(rootGameObject).await()

    DispatcherModelLoadObjectsDataEvent(
      objectsData = makeObjectsData(listOf(entranceObject))
    ).schedule(rootGameObject)
    gameObjectRegistry.add(entranceObject)
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

      val codec = protocol.getCodec(eventClass.createType()) as ICodec<IServerEvent>
      val event = codec.decode(buffer)
      logger.info { "Received server event: $event" }

      // val gameObject = TransientGameObject(command.objectId, TransientGameClass(id = -1, models = listOf()))
      val gameObject = gameObjectRegistry.get(command.objectId) ?: error("Game object ${command.objectId} not found")

      eventScheduler.processServerEvent(event, this, gameObject)
    }

    if(buffer.data.readableBytes() > 0) {
      logger.warn { "Buffer has ${buffer.data.readableBytes()} bytes left" }
    }
  }

  inline fun sendBatched(block: SpaceChannelOutgoingBatch.() -> Unit) {
    val batch = SpaceChannelOutgoingBatch()
    block(batch)
    sendBatchedImpl(batch)
  }

  fun sendBatchedImpl(batch: SpaceChannelOutgoingBatch) {
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

class EventScheduler {
  private val logger = KotlinLogging.logger { }

  fun process(event: IEvent, sender: SpaceChannel, gameObject: IGameObject<*>) {
    if(event is IClientEvent) {
      sender.sendBatched {
        event.attach(gameObject).enqueue()
      }
    } else {
      processServerEvent(event, sender, gameObject)
    }
  }

  fun processServerEvent(event: IEvent, sender: SpaceChannel, gameObject: IGameObject<*>) {
    logger.debug { "Processing server event: $event" }

    val systems = listOf(
      DispatcherSystem::class,
      LoginSystem::class,
    )

    for(system in systems) {
      val methods = system.declaredFunctions
      for(method in methods) {
        if(!method.hasAnnotation<OnEventFire>()) {
          logger.debug { "Method $method is not annotated with @OnEventFire" }
          continue
        }

        if(method.parameters.none { it.kind == KParameter.Kind.INSTANCE }) {
          throw IllegalArgumentException("Method $method is not an instance method")
        }

        val parameters = method.valueParameters

        val eventClass = parameters[0].type.kotlinClass
        if(eventClass != event::class) {
          logger.debug { "Method $method expects event of type ${eventClass.qualifiedName}, but received event is of type ${event::class.qualifiedName}" }
          continue
        }

        @Suppress("UNCHECKED_CAST")
        val nodeClass = parameters[1].type.kotlinClass as KClass<out Node>
        if(!nodeClass.isSubclassOf(Node::class)) {
          throw IllegalArgumentException("Method $method expects second parameter to be ${Node::class.qualifiedName}, but declared type is ${nodeClass.qualifiedName}")
        }

        val nodeBuilder = NodeBuilder()
        val nodeDefinition = nodeBuilder.getNodeDefinition(nodeClass)

        logger.info { "Trying to build node $nodeDefinition" }
        val node = nodeBuilder.tryBuild(nodeDefinition, gameObject.models.values.toSet())
                   ?: throw IllegalArgumentException("Failed to build node $nodeDefinition")
        node.init(sender, gameObject)
        logger.info { "Built node $node" }

        val instance = system.createInstance()

        logger.info { "Invoking ${system.qualifiedName}::${method.name} with event $event and node $node" }
        if(method.isSuspend) {
          GlobalScope.launch {
            method.callSuspend(instance, event, node)
          }
        } else {
          method.call(instance, event, node)
        }
      }
    }
  }
}

data class NodeDefinition(
  val clazz: KClass<out Node>,
  val components: List<ComponentDefinition>
)

data class ComponentDefinition(
  val parameter: KParameter,
  val type: KClass<out IModelConstructor>,
)

class NodeBuilder {
  private val logger = KotlinLogging.logger { }

  fun getComponents(clazz: KClass<out Node>): List<ComponentDefinition> {
    val constructor = clazz.primaryConstructor ?: throw IllegalArgumentException("Class $clazz has no primary constructor")
    val parameters = constructor.parameters
    for(parameter in parameters) {
      check(!parameter.type.isMarkedNullable) { "Parameter $parameter is marked as nullable" }
      check(!parameter.isVararg) { "Parameter $parameter is marked as vararg" }
      check(!parameter.isOptional) { "Parameter $parameter is marked as optional" }
      check(parameter.type.arguments.isEmpty()) { "Parameter $parameter has type arguments" }

      val root = IModelConstructor::class
      if(!parameter.type.kotlinClass.isSubclassOf(root)) {
        throw IllegalArgumentException("Parameter $parameter is not a subclass of ${root.qualifiedName}")
      }
    }

    return parameters.map { parameter ->
      @Suppress("UNCHECKED_CAST")
      val type = parameter.type.kotlinClass as KClass<out IModelConstructor>

      ComponentDefinition(parameter, type)
    }
  }

  fun getNodeDefinition(clazz: KClass<out Node>): NodeDefinition {
    val root = Node::class
    check(clazz.isSubclassOf(root)) { "Class $clazz is not a subclass of ${root.qualifiedName}" }

    val components = getComponents(clazz)
    return NodeDefinition(clazz, components)
  }

  fun collectNodeDefinitions(): List<NodeDefinition> {
    val nodeDefinitions = mutableListOf<NodeDefinition>()
    nodeDefinitions.add(getNodeDefinition(Node::class))

    ClassGraph().enableAllInfo().acceptPackages("org.araumi").scan().use { scanResult ->
      val classes = scanResult.getSubclasses(Node::class.qualifiedName).loadClasses().map { it.kotlin } as List<KClass<out Node>>
      logger.info { "Found ${classes.size} nodes" }

      for(clazz in classes) {
        logger.debug { "Discovered node: $clazz" }
        nodeDefinitions.add(getNodeDefinition(clazz))
      }
    }

    return nodeDefinitions
  }

  fun tryBuild(nodeDefinition: NodeDefinition, components: Set<IModelConstructor>): Node? {
    val constructor = nodeDefinition.clazz.primaryConstructor ?: return null
    val parameters = constructor.parameters

    val args = mutableMapOf<KParameter, IModelConstructor>()
    for(parameter in parameters) {
      val component = components.firstOrNull { it::class == parameter.type.kotlinClass }
      if(component == null) {
        logger.warn { "Component $parameter not found" }
        return null
      }

      args[parameter] = component
    }

    return constructor.callBy(args)
  }
}

class SpaceChannelOutgoingBatch {
  val commands: MutableList<SpaceCommand> = mutableListOf()

  fun SpaceCommand.enqueue() {
    commands.add(this)
  }
}
