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

package jp.assasans.narukami.server.core

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base class for all systems.
 * Systems process events and contain game logic.
 *
 * @see [jp.assasans.narukami.server.core.ArchitectureDocs]
 */
abstract class AbstractSystem : KoinComponent {
  val eventScheduler: IEventScheduler by inject()
}

/**
 * Marks a function within an [AbstractSystem] as an event handler.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnEventFire

/**
 * Requires that an event handler be executed for each event received,
 * meaning that its nodes must always build successfully.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Mandatory

/**
 * Allows an event handler to run in parallel with another event handlers,
 * required when using awaiting objects or dependencies.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OutOfOrderExecution

/**
 * Marks a node to be searched for in an entire object registry of the space.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class JoinAll

/**
 * Marks a node to be searched for in all existing space channels of the space.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class JoinAllChannels

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnlySpaceContext

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnlyLoadedObjects
