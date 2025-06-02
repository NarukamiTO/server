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
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.net.SpaceChannel
import jp.assasans.narukami.server.net.command.ProtocolClass
import jp.assasans.narukami.server.net.command.ProtocolModel
import jp.assasans.narukami.server.net.sessionNotNull
import jp.assasans.narukami.server.res.Resource

// TODO: Use IPending<T>
@ProtocolModel(-1)
data class DeferredDependenciesCC(
  val callbackId: Int,
  val deferred: CompletableDeferred<Unit>,
) : IModelConstructor

@ProtocolClass(-1)
data class DeferredDependenciesTemplate(
  val deferredDependencies: DeferredDependenciesCC,
) : ITemplate

class DispatcherNode(
  val dispatcher: DispatcherModelCC,
) : Node()

class DispatcherWithMutexNode(
  val dispatcher: DispatcherModelCC,
  val dispatcherMutex: DispatcherMutexComponent,
) : Node()

data class DispatcherMutexComponent(val mutexes: WeakHashMap<SpaceChannel, Mutex>) : IComponent

class DispatcherSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun preloadResourcesWrapped(
    event: PreloadResourcesWrappedEvent<*>,
    any: Node,
    @JoinAll dispatcher: DispatcherNode,
  ) {
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

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun loadDependenciesManaged(event: DispatcherLoadDependenciesManagedEvent, dispatcher: DispatcherWithMutexNode) {
    logger.info { "Load dependencies managed: $event" }

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
    dispatcher.context.space.objects.add(deferredDependenciesObject)

    // The client can only track one dependency load batch at a time
    // (see [DispatcherModel::loadDependencies] and [DispatcherModel::onBatchLoadingComplete]).
    // Otherwise, the older batch will never inform the server of its completion.
    val mutex = dispatcher.dispatcherMutex.mutexes.getOrPut(dispatcher.context.requireSpaceChannel) { Mutex() }
    if(mutex.isLocked) logger.info { "Dispatcher mutex is locked for ${dispatcher.context.requireSpaceChannel}, waiting for unlock..." }
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
  }

  @OnEventFire
  @Mandatory
  fun dependenciesLoaded(event: DispatcherModelDependenciesLoadedEvent, dispatcher: DispatcherWithMutexNode) {
    logger.debug { "Dependencies loaded: ${event.callbackId}" }

    val deferredDependenciesObject = dispatcher.context.space.objects.get(event.callbackId.toLong())
                                     ?: error("Deferred dependencies object ${event.callbackId} not found")

    val mutex = dispatcher.dispatcherMutex.mutexes[dispatcher.context.requireSpaceChannel]
                ?: error("Mutex not found for ${dispatcher.context.requireSpaceChannel}")
    mutex.unlock()

    val deferredDependencies = deferredDependenciesObject.adaptSingle<DeferredDependenciesCC>(dispatcher.context)
    deferredDependencies.deferred.complete(Unit)
    dispatcher.context.space.objects.remove(deferredDependenciesObject)
    logger.debug { "Deferred dependencies $deferredDependencies resolved" }
  }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun loadObjectsManaged(event: DispatcherLoadObjectsManagedEvent, dispatcher: DispatcherNode) {
    logger.info { "Load objects managed: $event" }

    DispatcherLoadDependenciesManagedEvent(
      classes = event.objects.map { it.parent },
      resources = event.objects.flatMap { gameObject ->
        gameObject.models.values.flatMap { model ->
          model.provide(gameObject, dispatcher.context).getResources()
        }
      }
    ).schedule(dispatcher).await()

    DispatcherModelLoadObjectsDataEvent(
      objectsData = ObjectsData.new(event.objects, dispatcher.context)
    ).schedule(dispatcher)

    val context = dispatcher.context
    if(context is SpaceChannelModelContext) {
      context.channel.loadedObjects.addAll(event.objects.map { it.id })
    }

    logger.debug { "Objects loaded: $event" }
    event.deferred.complete(Unit)
  }

  @OnEventFire
  @Mandatory
  fun unloadObjects(event: DispatcherUnloadObjectsManagedEvent, dispatcher: DispatcherNode) {
    DispatcherModelUnloadObjectsEvent(event.objects).schedule(dispatcher)

    val context = dispatcher.context
    if(context is SpaceChannelModelContext) {
      context.channel.loadedObjects.removeAll(event.objects.map { it.id })
    }

    logger.debug { "Objects unloaded: $event" }
  }

  @OnEventFire
  @Mandatory
  suspend fun openSpace(event: DispatcherOpenSpaceEvent, dispatcher: DispatcherNode) {
    logger.info { "Open space channel: $event" }

    // Space management is too low-level for the Systems API,
    // we just bridge event to control channel API.
    val session = dispatcher.context.requireSpaceChannel.sessionNotNull
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
