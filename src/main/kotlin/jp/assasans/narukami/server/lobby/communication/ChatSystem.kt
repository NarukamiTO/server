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

package jp.assasans.narukami.server.lobby.communication

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.lobby.SessionLogoutEvent
import jp.assasans.narukami.server.lobby.UserNode

data class ChatNode(
  val chat: ChatModelCC,
) : Node()

class ChatSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  fun chatAdded(event: NodeAddedEvent, chat: ChatNode) {
    ChatModelShowMessagesEvent(
      messages = listOf(
        ChatMessage(
          addressMode = ChatAddressMode.PUBLIC_TO_ALL,
          battleLinks = emptyList(),
          channel = null,
          links = null,
          messageType = MessageType.SYSTEM,
          sourceUser = null,
          targetUser = null,
          text = "стена текста в стиле тх",
          timePassedInSec = 0
        )
      )
    ).schedule(chat)
  }

  @OnEventFire
  fun sendMessage(
    event: ChatModelSendMessageEvent,
    chat: ChatNode,
    @JoinAll user: UserNode,
    @JoinAll dispatcher: DispatcherNode,
  ) {
    val message = ChatMessage(
      addressMode = event.addressMode,
      battleLinks = emptyList(),
      channel = event.channel,
      links = null,
      messageType = MessageType.USER,
      sourceUser = UserStatus(
        chatModeratorLevel = ChatModeratorLevel.ADMINISTRATOR,
        ip = "",
        rankIndex = 1,
        uid = user.username.username,
        userId = user.gameObject.id
      ),
      targetUser = null,
      text = event.text,
      timePassedInSec = 0
    )

    // Slash-prefixed commands are reserved for the client-side commands.
    // Server uses vim-like commands, prefixed with colon.
    if(event.text.startsWith(":")) {
      when(val command = event.text.substring(1)) {
        "logout" -> {
          logger.debug { "Logout command received, closing session" }
          SessionLogoutEvent().schedule(dispatcher)
        }

        else     -> {
          logger.warn { "Unknown command: $command" }
        }
      }
      return
    }

    ChatModelShowMessagesEvent(messages = listOf(message)).schedule(chat)
  }

  @OnEventFire
  fun changeChannel(event: ChatModelChangeChannelEvent, chat: ChatNode) {
    logger.debug { "Changed channel: $event" }
  }
}
