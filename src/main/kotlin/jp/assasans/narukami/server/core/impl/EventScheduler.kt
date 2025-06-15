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

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.battlefield.replay.BattlefieldReplayMiddleware
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.extensions.singleOrNullOrThrow
import jp.assasans.narukami.server.net.sessionNotNull

data class ScheduledEvent(
  val event: IEvent,
  val context: IModelContext,
  val gameObject: IGameObject
)

class EventScheduler(private val scope: CoroutineScope) : IEventScheduler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val eventQueue = Channel<ScheduledEvent>(Channel.UNLIMITED)
  private val eventJob = scope.launch {
    for(scheduledEvent in eventQueue) {
      try {
        process(scheduledEvent.event, scheduledEvent.context, scheduledEvent.gameObject)
      } catch(exception: Exception) {
        logger.error(exception) { "Error processing event $scheduledEvent" }
      }
    }
  }

  override fun schedule(event: IEvent, context: IModelContext, gameObject: IGameObject) {
    val scheduledEvent = ScheduledEvent(event, context, gameObject)

    logger.debug { "Scheduling event $scheduledEvent" }
    eventQueue.trySend(scheduledEvent).onFailure {
      logger.error(it) { "Failed to schedule event $scheduledEvent" }
    }
  }

  private suspend fun process(event: IEvent, context: IModelContext, gameObject: IGameObject) {
    if(event is IClientEvent) {
      // delay(Random.nextLong(100, 500))
      context.requireSpaceChannel.sendBatched {
        event.attach(gameObject).enqueue()
      }
    } else {
      processServerEvent(event, context, gameObject)
    }
  }

  data class NodeParameterDefinition(
    val parameter: KParameter,
    val nodeDefinition: NodeDefinition,
    val isList: Boolean,
  ) {
    val joinAll = parameter.hasAnnotation<JoinAll>()
    val joinBy = parameter.annotations
      .filterIsInstance<JoinBy>()
      .map { it.component }
      .singleOrNullOrThrow()
    val perChannel = parameter.hasAnnotation<PerChannel>()
    val allowUnloaded = parameter.hasAnnotation<AllowUnloaded>()
  }

  data class EventHandlerDefinition(
    val event: KClass<out IEvent>,
    val system: KClass<out AbstractSystem>,
    val function: KFunction<*>,
    val nodes: List<NodeParameterDefinition>,
  ) {
    val mandatory = function.hasAnnotation<Mandatory>()
    val outOfOrder = function.hasAnnotation<OutOfOrderExecution>()
    val onlySpaceContext = function.hasAnnotation<OnlySpaceContext>()
  }

  private val sessions: ISessionRegistry by inject()

  private val nodeBuilder = NodeBuilder()
  private val systems: List<KClass<out AbstractSystem>>
  private val handlers: List<EventHandlerDefinition>
  private val middleware: List<EventMiddleware>

  init {
    systems = ClassGraph()
      .enableAllInfo()
      .acceptPackages("jp.assasans.narukami.server")
      .scan()
      .use { scanResult ->
        scanResult.getSubclasses(AbstractSystem::class.java).mapNotNull { classInfo ->
          @Suppress("UNCHECKED_CAST")
          val clazz = classInfo.loadClass().kotlin as KClass<out AbstractSystem>
          logger.info { "Discovered system: $clazz" }

          clazz
        }
      }

    handlers = systems.flatMap { system ->
      system.declaredFunctions
        .filter { function -> function.hasAnnotation<OnEventFire>() }
        .map { function -> makeHandlerDefinition(system, function) }
    }

    for(handler in handlers) {
      logger.info { "Discovered event handler: ${handler.system.qualifiedName}::${handler.function.name} for ${handler.event.qualifiedName} with ${handler.nodes.map { it.nodeDefinition }}" }
    }

    middleware = listOf(
      BattlefieldReplayMiddleware,
    )
  }

  fun makeHandlerDefinition(system: KClass<out AbstractSystem>, function: KFunction<*>): EventHandlerDefinition {
    val parameters = function.valueParameters
    val eventClass = parameters[0].type.kotlinClass
    if(!eventClass.isSubclassOf(IEvent::class)) {
      throw IllegalArgumentException("${system.qualifiedName}::${function.name} first parameter is not an event type, got $eventClass")
    }

    val nodeParameters = parameters.drop(1)
    val nodes = nodeParameters.map { parameter ->
      var type = parameter.type
      var isList = false
      if(type.kotlinClass.isSubclassOf(List::class)) {
        type = requireNotNull(type.arguments[0].type) {
          "${system.qualifiedName}::${function.name} parameter ${parameter.name} has an illegal List<T> type argument"
        }
        isList = true
      }
      if(!type.kotlinClass.isSubclassOf(Node::class)) {
        throw IllegalArgumentException("${system.qualifiedName}::${function.name} parameter ${parameter.name} illegal type $type")
      }

      val nodeDefinition = nodeBuilder.getNodeDefinition(type)
      NodeParameterDefinition(
        parameter,
        nodeDefinition,
        isList,
      )
    }

    @Suppress("UNCHECKED_CAST")
    return EventHandlerDefinition(
      event = eventClass as KClass<out IEvent>,
      system,
      function,
      nodes,
    )
  }

  private suspend fun processServerEvent(event: IEvent, context: IModelContext, gameObject: IGameObject) {
    logger.info { "Processing server event: $event on $context" }

    val startTotal = Clock.System.now()
    for(middleware in middleware) {
      try {
        logger.trace { "Processing $event by middleware $middleware" }
        middleware.process(this, event, gameObject, context)
      } catch(exception: Exception) {
        logger.error(exception) { "Error processing middleware $middleware for $event" }
      }
    }

    var startResolve: Instant
    val durationsResolve = mutableListOf<Duration>()

    var handled = false
    for(handler in handlers) {
      startResolve = Clock.System.now()
      if(!event::class.isSubclassOf(handler.event)) continue

      if(handler.onlySpaceContext && context !is SpaceModelContext) {
        logger.debug { "Skipping handler ${handler.system.qualifiedName}::${handler.function.name} for $event, requires space context, but got $context" }
        continue
      }
      if(!handler.onlySpaceContext && context is SpaceModelContext) {
        logger.debug { "Skipping handler ${handler.system.qualifiedName}::${handler.function.name} for $event, requires space channel context, but got $context" }
        continue
      }

      logger.debug { "Trying handler ${handler.system.qualifiedName}::${handler.function.name} for $event" }

      val args = mutableMapOf<KParameter, Any?>()
      args[handler.function.valueParameters[0]] = event

      // Client-to-server events always have exactly 1 context object attached by the client.
      // We use Set<T> to avoid duplicates (e.g., in case event is scheduled to the user object).
      val contextObjects = mutableSetOf(gameObject)

      // If present, we also attach a user object
      if(context is SpaceChannelModelContext) {
        val user = context.channel.sessionNotNull.user
        if(user != null) {
          logger.debug { "Attached user object to the context of $event" }
          contextObjects.add(user)
        }
      }

      if(!buildNodes(context, contextObjects, handler, args)) {
        continue
      }

      val instanceParameter = requireNotNull(handler.function.instanceParameter) {
        "${handler.system.qualifiedName}::${handler.function.name} is not an instance method"
      }
      val instance = handler.system.createInstance()
      args[instanceParameter] = instance

      durationsResolve.add(Clock.System.now() - startResolve)
      invokeHandler(event, context, handler, args)
      handled = true
    }

    if(!handled) {
      logger.warn { "Unhandled $event on $context" }
    }

    val durationTotal = Clock.System.now() - startTotal
    val durationResolve = durationsResolve.fold(Duration.ZERO) { acc, duration -> acc + duration }
    logger.trace { "Processing ${event::class.qualifiedName} took (resolve) $durationResolve" }
    logger.trace { "Processing ${event::class.qualifiedName} took (total) $durationTotal" }
    if(durationResolve > 500.microseconds) {
      logger.warn { "Event ${event::class.qualifiedName} took (resolve) $durationResolve" }
    }
  }

  /**
   * @return `true` if all nodes were built successfully
   */
  private fun buildNodes(
    context: IModelContext,
    contextObjects: Set<IGameObject>,
    handler: EventHandlerDefinition,
    args: MutableMap<KParameter, Any?>
  ): Boolean {
    var previousObjects: Set<IGameObject>? = null
    for(nodeParameter in handler.nodes) {
      val (parameter, nodeDefinition) = nodeParameter

      val unfilteredObjects = if(nodeParameter.joinAll) {
        context.space.objects.all
      } else {
        contextObjects
      }

      val objects = if(nodeParameter.joinBy != null) {
        requireNotNull(previousObjects) { "JoinBy requires previous node to act as a key" }
        val previousObject = if(previousObjects.size == 1) {
          previousObjects.single()
        } else {
          throw IllegalArgumentException("JoinBy requires exactly one key object, got ${previousObjects.size}")
        }

        val keyGroup = previousObject.getComponent(nodeParameter.joinBy)
        unfilteredObjects.filter { gameObject ->
          val targetGroup = gameObject.getComponentOrNull(nodeParameter.joinBy)
          logger.info { "Wants $keyGroup, got $targetGroup, pass: ${targetGroup == keyGroup}" }
          targetGroup == keyGroup
        }.toSet()
      } else {
        unfilteredObjects
      }

      val contexts = if(nodeParameter.perChannel) {
        requireNotNull(nodeParameter.isList) { "@${PerChannel::class.simpleName} can only be used with List<T> parameters" }
        // TODO: Do we need [ISpace.channels]?
        sessions.all
          .mapNotNull { session -> session.spaces.get(context.space.id) }
          .map { channel -> SpaceChannelModelContext(channel) }
      } else {
        listOf(context)
      }

      // TODO: This is very inefficient (node-context-object triple loop), should do something to it
      val nodes = mutableListOf<Node>()
      for(context in contexts) {
        for(gameObject in objects) {
          if(!nodeParameter.allowUnloaded) {
            if(context is SpaceChannelModelContext && !context.channel.loadedObjects.contains(gameObject.id)) {
              logger.trace { "[${nodeDefinition.type.kotlinClass.simpleName}] $gameObject is not loaded in ${context.channel}, excluding" }
              continue
            }
          }

          val node = nodeBuilder.tryBuildLazy(nodeDefinition, gameObject, context)
          if(node != null) {
            nodes.add(node)
            logger.trace { "[${nodeDefinition.type.kotlinClass.simpleName}] Built node $node for $gameObject in $context" }
          } else {
            logger.trace { "[${nodeDefinition.type.kotlinClass.simpleName}] Failed to build for $gameObject in $context" }
          }
        }
      }

      if(nodeParameter.isList) {
        args[parameter] = nodes
        logger.trace { "[${nodeDefinition.type.kotlinClass.simpleName}] Built nodes $nodes for '${parameter.name}' of ${handler.system.qualifiedName}::${handler.function.name}" }
      } else {
        val node = when(nodes.size) {
          0    -> {
            if(handler.mandatory) throw IllegalArgumentException("Failed to build node $nodeDefinition for '${parameter.name}' of ${handler.system.qualifiedName}::${handler.function.name}")

            logger.trace { "[${nodeDefinition.type.kotlinClass.simpleName}] Failed to build for '${parameter.name}' of ${handler.system.qualifiedName}::${handler.function.name}" }
            return false
          }

          1    -> nodes[0]
          else -> throw IllegalArgumentException("Expected one game object for '${parameter.name}' of ${handler.system.qualifiedName}::${handler.function.name}, got ${nodes.size}")
        }

        args[parameter] = node
        logger.trace { "[${nodeDefinition.type.kotlinClass.simpleName}] Built node $node for '${parameter.name}' of ${handler.system.qualifiedName}::${handler.function.name}" }
      }

      previousObjects = nodes.gameObjects.toSet()
    }

    return true
  }

  private suspend fun invokeHandler(
    event: IEvent,
    context: IModelContext,
    handler: EventHandlerDefinition,
    args: Map<KParameter, Any?>,
  ) {
    logger.trace { "Invoking ${handler.system.qualifiedName}::${handler.function.name} with ${args.mapKeys { (parameter, _) -> parameter.name }}" }
    if(handler.function.isSuspend) {
      if(handler.outOfOrder) {
        context.requireSpaceChannel.socket.launch {
          val timeout = 5.seconds
          val job = scope.launch {
            delay(timeout)
            logger.warn { "${handler.system.qualifiedName}::${handler.function.name} timed out ($timeout) while processing $event" }
          }

          try {
            handler.function.callSuspendBy(args)
            logger.trace { "${handler.system.qualifiedName}::${handler.function.name} processed $event" }
          } finally {
            job.cancelAndJoin()
          }
        }
      } else {
        val timeout = 5.seconds
        val job = scope.launch {
          delay(timeout)
          logger.error { "${handler.system.qualifiedName}::${handler.function.name} timed out ($timeout) while processing $event" }
          logger.error { "Possible deadlock detected, try marking event handler with @${OutOfOrderExecution::class.simpleName} to not suspend incoming command queue" }
        }

        try {
          handler.function.callSuspendBy(args)
          logger.trace { "${handler.system.qualifiedName}::${handler.function.name} processed $event" }
        } finally {
          job.cancelAndJoin()
        }
      }
    } else {
      handler.function.callBy(args)
    }
  }
}
