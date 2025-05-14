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

import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.lobby.user.adaptSingle
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

class DispatcherSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  @Mandatory
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
  suspend fun loadDependenciesManaged(event: DispatcherLoadDependenciesManagedEvent, dispatcher: DispatcherNode) {
    logger.info { "Load dependencies managed: $event" }

    // TODO: Use a better ID source, it is used as temporary object ID,
    //  so either it should be unique, or we need to have some object namespacing
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
    dispatcher.context.space.objects.add(deferredDependenciesObject)

    DispatcherModelLoadDependenciesEvent(
      dependencies = ObjectsDependencies.new(
        callbackId = event.callbackId,
        classes = event.classes,
        // Loading more than one resource with the same ID will cause a deadlock on the client.
        // TODO: This may cause bugs when same resource loaded in both Lazy and Eager modes, we should prefer Eager.
        resources = event.resources.distinctBy { it.id }
      )
    ).schedule(dispatcher)
  }

  @OnEventFire
  @Mandatory
  suspend fun dependenciesLoaded(event: DispatcherModelDependenciesLoadedEvent, dispatcher: DispatcherNode) {
    logger.info { "Dependencies loaded: ${event.callbackId}" }

    val deferredDependenciesObject = dispatcher.context.space.objects.get(event.callbackId.toLong())
                                     ?: error("Deferred dependencies object ${event.callbackId} not found")
    val deferredDependencies = deferredDependenciesObject.adaptSingle<DeferredDependenciesCC>(dispatcher.context)
    deferredDependencies.deferred.complete(Unit)
    dispatcher.context.space.objects.remove(deferredDependenciesObject)
    logger.info { "Deferred dependencies $deferredDependencies resolved" }
  }

  @OnEventFire
  @Mandatory
  @OutOfOrderExecution
  suspend fun loadObjectsManaged(event: DispatcherLoadObjectsManagedEvent, dispatcher: DispatcherNode) {
    logger.info { "Load objects managed: $event" }

    // TODO: Use a better ID source, it is used as temporary object ID,
    //  so either it should be unique, or we need to have some object namespacing
    event.callbackId = event.deferred.hashCode()

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

    logger.info { "Objects loaded: $event" }
    event.deferred.complete(Unit)
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

    logger.info { "Space channel opened: $channel" }
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
