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

package org.araumi.server.lobby.communication

import org.araumi.server.core.IClientEvent
import org.araumi.server.core.IModelConstructor
import org.araumi.server.core.IServerEvent
import org.araumi.server.net.command.*

@ProtocolModel(6071565290933648049)
data class ChatModelCC(
  val admin: Boolean,
  val antifloodEnabled: Boolean,
  val bufferSize: Int,
  val channels: List<String>,
  val chatEnabled: Boolean,
  val chatModeratorLevel: ChatModeratorLevel,
  val linksWhiteList: List<String>,
  val minChar: Int,
  val minWord: Int,
  val privateMessagesEnabled: Boolean,
  val selfName: String,
  val showLinks: Boolean,
  val typingSpeedAntifloodEnabled: Boolean,
) : IModelConstructor

@ProtocolEvent(4202027557179282961)
data class ChatModelShowMessagesEvent(
  val messages: List<ChatMessage>,
) : IClientEvent

@ProtocolEvent(3122753540375943279)
data class ChatModelSendMessageEvent(
  val targetUserName: String,
  val addressMode: ChatAddressMode,
  val channel: String,
  val text: String,
) : IServerEvent

@ProtocolEvent(6683616035809206555)
data class ChatModelChangeChannelEvent(
  val channel: String,
) : IServerEvent

@ProtocolStruct
data class ChatMessage(
  val addressMode: ChatAddressMode,
  val battleLinks: List<BattleChatLink>,
  val channel: String?,
  val links: List<String>?,
  val messageType: MessageType,
  val sourceUser: UserStatus?,
  val targetUser: UserStatus?,
  val text: String,
  val timePassedInSec: Int,
)

@ProtocolEnum
enum class ChatAddressMode(override val value: Int) : IProtocolEnum<Int> {
  PUBLIC_TO_ALL(0),
  PUBLIC_ADDRESSED(1),
  PRIVATE(2),
}

@ProtocolStruct
data class BattleChatLink(
  val battleIdHex: String,
  val battleMode: String,
  val battleName: String,
  val link: String?,
)

@ProtocolEnum
enum class MessageType(override val value: Int) : IProtocolEnum<Int> {
  USER(0),
  SYSTEM(1),
  WARNING(2),
}

@ProtocolStruct
data class UserStatus(
  val chatModeratorLevel: ChatModeratorLevel,
  val ip: String,
  val rankIndex: Int,
  val uid: String,
  val userId: Long,
)

@ProtocolEnum
enum class ChatModeratorLevel(override val value: Int) : IProtocolEnum<Int> {
  NONE(0),
  COMMUNITY_MANAGER(1),
  BATTLE_ADMINISTRATOR(2),
  BATTLE_MODERATOR(3),
  BATTLE_CANDIDATE(4),
  ADMINISTRATOR(5),
  MODERATOR(6),
  CANDIDATE(7),
  EVENT_CHAT_ADMIN(8),
  EVENT_CHAT_MODERATOR(9),
  EVENT_CHAT_CANDIDATE(10),
}
