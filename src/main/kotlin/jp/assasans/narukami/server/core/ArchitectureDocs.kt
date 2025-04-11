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

/**
 * # Architecture
 *
 * Server architecture is largely based on the Tanki X's *Entity Component System* architecture.
 * There are [Systems][jp.assasans.narukami.server.core.AbstractSystem] that process [Events][jp.assasans.narukami.server.core.IEvent],
 * and [Nodes][jp.assasans.narukami.server.core.Node] that are used for selecting data for event handlers.
 *
 * ## Sessions
 *
 * [Session][jp.assasans.narukami.server.net.session.ISession] represents a unique client instance connected to the server.
 * It is identified by a session hash, contains a control channel and zero or more space channels.
 *
 * ## Spaces
 *
 * Space is a room, containing own set of game objects and space channels.
 * Client can be connected to multiple spaces at the same time.
 *
 * It is mostly at the server's discretion how to divide objects between spaces.
 * The only exception is the requirement to have a separate space per battle,
 * due to abuse of the root object on the client.
 *
 * The server requests the client to open a space, while the client can
 * open a space without a server request, the official client does not do this.
 *
 * Technically, spaces can be hosted on different servers,
 * but this server does not implement this feature.
 *
 * ### Space channels
 *
 * Space channel is a networking primitive of the game,
 * it represents a single client session that is connected to a space.
 *
 * ## Game Objects
 *
 * [Game Objects][jp.assasans.narukami.server.core.IGameObject] mimic *Entities* in ECS.
 * They have a unique ID, can be attached to zero or more spaces, and store models.
 *
 * Only the server can manage game objects, the client cannot create or destroy them.
 *
 * ## Models
 *
 * [Models][jp.assasans.narukami.server.core.IModelConstructor] are the main concept of the game architecture.
 * They are contained in Game Objects, and used as *Components* in ECS,
 * but are coupled with logic (Systems) on the client (decoupled on the server).
 *
 * ## Events
 *
 * Events (called *methods* on the client) can be received and [Scheduled][jp.assasans.narukami.server.core.schedule].
 * Scheduling an event means processing it (for example, sending it to the client in the case of a client event).
 *
 * [Client Events][jp.assasans.narukami.server.core.IClientEvent] are used for server-to-client communication,
 * they are sent to the client when scheduled.
 *
 * [Server Events][jp.assasans.narukami.server.core.IServerEvent] are used for client-to-server communication,
 * they are scheduled when the client sends them to the server.
 *
 * Events that inherit directly from [Event][jp.assasans.narukami.server.core.IEvent] are used for
 * internal server communication, usually to abstract or reuse logic.
 */
@Suppress("unused")
private data object ArchitectureDocs
