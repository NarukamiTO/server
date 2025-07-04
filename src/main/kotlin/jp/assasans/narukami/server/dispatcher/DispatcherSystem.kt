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

package jp.assasans.narukami.server.dispatcher

import java.util.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import jp.assasans.narukami.server.battlefield.replay.ReplaySocketClient
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.entrance.DispatcherNodeV2
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.lobby.UsernameComponent
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.session.userNotNull
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.protocol.ProtocolModel
import jp.assasans.narukami.server.res.Resource

// TODO: Use IPending<T>
@ProtocolModel(-1)
data class DeferredDependenciesCC(
  val callbackId: Int,
  val deferred: CompletableDeferred<Unit>,
) : IModelConstructor

object DeferredDependenciesTemplate : TemplateV2() {
  fun create(id: Long, event: DispatcherLoadDependenciesManagedEvent) = gameObject(id).apply {
    addModel(
      DeferredDependenciesCC(
        callbackId = event.callbackId,
        deferred = event.deferred
      )
    )
  }
}

class DispatcherNode(
  val dispatcher: DispatcherModelCC,
) : Node() {
  override fun toString(): String {
    return "DispatcherNode(username=${context.requireSpaceChannel.sessionNotNull.userNotNull.getComponent<UsernameComponent>().username})"
  }
}

class DispatcherWithMutexNode(
  val dispatcher: DispatcherModelCC,
  val dispatcherMutex: DispatcherMutexComponent,
) : Node()

data class DispatcherMutexComponent(val mutexes: WeakHashMap<SpaceChannel, Mutex>) : IComponent

class DispatcherSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun preloadResourcesWrapped(
    context: IModelContext,
    event: PreloadResourcesWrappedEvent<*>,
    any: NodeV2,
    @JoinAll dispatcher: DispatcherNodeV2,
  ) = context {
    val resources = event.inner::class
      .declaredMemberProperties
      .filter { it.returnType.kotlinClass.isSubclassOf(Resource::class) }
      .map { it.getter.call(event.inner) as Resource<*, *> }
      .toList()
    logger.debug { "Preload resources: $resources" }

    DispatcherLoadDependenciesManagedEvent(
      classes = listOf(),
      resources = resources
    ).schedule(dispatcher).await()
    logger.debug { "Resources preloaded" }

    event.inner.schedule(any)
  }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun loadDependenciesManaged(
    context: IModelContext,
    event: DispatcherLoadDependenciesManagedEvent,
    dispatcher: DispatcherNodeV2,
  ) = context {
    logger.info { "Load dependencies managed: $event" }

    val deferredDependenciesObject = DeferredDependenciesTemplate.create(id = event.callbackId.toLong(), event)
    context.space.objects.add(deferredDependenciesObject)

    // The client can only track one dependency load batch at a time
    // (see [DispatcherModel::loadDependencies] and [DispatcherModel::onBatchLoadingComplete]).
    // Otherwise, the older batch will never inform the server of its completion.
    val mutex = dispatcher.dispatcherMutex.mutexes.getOrPut(context.requireSpaceChannel) { Mutex() }
    if(mutex.isLocked) logger.info { "Dispatcher mutex is locked for ${context.requireSpaceChannel}, waiting for unlock..." }
    mutex.lock()

    DispatcherModelLoadDependenciesEvent(
      dependencies = ObjectsDependencies.new(
        callbackId = event.callbackId,
        classes = event.classes,
        // Loading more than one resource with the same ID will cause a deadlock on the client.
        // TODO: This may cause bugs when same resource loaded in both Lazy and Eager modes, we should prefer Eager.
        resources = event.resources.distinctBy { it.id }
      )
    ).schedule(dispatcher)

    logger.debug { "Dependencies ${event.callbackId} ${event.resources.map { it.id.id }} load request scheduled" }

    if(context.requireSpaceChannel.socket is ReplaySocketClient) {
      logger.info { "Simulating dependencies load for replay socket client: $event" }
      mutex.unlock()
      event.deferred.complete(Unit)
    }
  }

  @OnEventFireV2
  fun dependenciesLoaded(
    context: IModelContext,
    event: DispatcherModelDependenciesLoadedEvent,
    dispatcher: DispatcherNodeV2,
  ) {
    if(context.requireSpaceChannel.socket is ReplaySocketClient) {
      logger.info { "Skipping dependencies loaded event for replay socket client: $event" }
      return
    }

    logger.debug { "Dependencies loaded: ${event.callbackId}" }

    val deferredDependenciesObject = context.space.objects.get(event.callbackId.toLong())
                                     ?: error("Deferred dependencies object ${event.callbackId} not found")

    val mutex = dispatcher.dispatcherMutex.mutexes[context.requireSpaceChannel]
                ?: error("Mutex not found for ${context.requireSpaceChannel}")
    mutex.unlock()

    val deferredDependencies = deferredDependenciesObject.adaptSingle<DeferredDependenciesCC>(context)
    deferredDependencies.deferred.complete(Unit)
    context.space.objects.remove(deferredDependenciesObject)
    logger.debug { "Deferred dependencies $deferredDependencies resolved" }
  }

  @OnEventFireV2
  @OutOfOrderExecution
  suspend fun loadObjectsManaged(
    context: IModelContext,
    event: DispatcherLoadObjectsManagedEvent,
    dispatcher: DispatcherNodeV2,
  ) = context {
    logger.info { "Load objects managed: $event" }

    DispatcherLoadDependenciesManagedEvent(
      classes = event.objects.map { it.parent },
      resources = event.objects.flatMap { gameObject ->
        gameObject.models.values.flatMap { model ->
          model.provide(gameObject, context).getResources()
        }
      }
    ).schedule(dispatcher).await()

    DispatcherModelLoadObjectsDataEvent(
      objectsData = ObjectsData.new(event.objects, context)
    ).schedule(dispatcher)

    val context = context
    if(context is SpaceChannelModelContext) {
      logger.trace { "Adding loaded objects to channel: ${event.objects.map { it.id }}" }
      context.channel.loadedObjects.addAll(event.objects.map { it.id })
    }

    logger.debug { "Objects loaded: $event" }
    event.deferred.complete(Unit)
  }

  @OnEventFireV2
  fun unloadObjects(
    context: IModelContext,
    event: DispatcherUnloadObjectsManagedEvent,
    dispatcher: DispatcherNodeV2,
  ) = context {
    DispatcherModelUnloadObjectsEvent(event.objects).schedule(dispatcher)

    if(context is SpaceChannelModelContext) {
      context.channel.loadedObjects.removeAll(event.objects.map { it.id })
    }

    logger.debug { "Objects unloaded: $event" }
  }

  @OnEventFireV2
  suspend fun openSpace(
    context: IModelContext,
    event: DispatcherOpenSpaceEvent,
    dispatcher: DispatcherNodeV2,
  ) {
    logger.info { "Open space channel: $event" }

    // Space management is too low-level for the Systems API,
    // we just bridge event to control channel API.
    val session = context.requireSpaceChannel.sessionNotNull
    val channel = session.controlChannel.openSpace(event.id).await()
    event.deferred.complete(channel)

    logger.debug { "Space channel opened: $channel" }
  }
}

/**
 * Wraps an event with [PreloadResourcesWrappedEvent] to load contained
 * in the event resources on the client before scheduling the actual event.
 *
 * Used when client event contains resources that need to be loaded beforehand,
 * e.g. in [jp.assasans.narukami.server.entrance.EntranceAlertModelShowAlertEvent].
 */
fun <T : IEvent> T.preloadResources(): PreloadResourcesWrappedEvent<T> {
  return PreloadResourcesWrappedEvent(this)
}
