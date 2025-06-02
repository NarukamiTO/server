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
import jp.assasans.narukami.server.NOTICE_SHORT
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.dispatcher.DispatcherNode
import jp.assasans.narukami.server.lobby.SessionLogoutEvent
import jp.assasans.narukami.server.lobby.UserNode
import jp.assasans.narukami.server.net.sessionNotNull

data class ChatNode(
  val chat: ChatModelCC,
) : Node()

class ChatSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFire
  fun chatAdded(event: NodeAddedEvent, chat: ChatNode) {
    val identity = chat.context.requireSpaceChannel.sessionNotNull.properties["identity"]?.split(",") ?: emptyList()
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
          text = buildString {
            append("<font size='10'>")
            append(NOTICE_SHORT)
            append("</font>\n\n")

            if(identity.isNotEmpty()) append("Client identity: ${identity.joinToString(", ")}")
            if(!identity.contains("baseline")) {
              append("<font color='#ee4e3e'><b>")
              appendLine("WARNING!")
              appendLine("You do not use <u>baseline</u> branch.")
              append("GAME WILL HAVE BUGS AND MAY BE UNPLAYABLE!")
              append("</b></font>")
            }
          },
          timePassedInSec = 0
        )
      )
    ).schedule(chat)
  }

  @OnEventFire
  @Mandatory
  fun sendMessage(
    event: ChatModelSendMessageEvent,
    chat: ChatNode,
    user: UserNode,
    @JoinAll dispatcher: DispatcherNode,
    @JoinAllChannels chats: List<ChatNode>,
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

    ChatModelShowMessagesEvent(messages = listOf(message)).schedule(chats)
  }

  @OnEventFire
  @Mandatory
  fun changeChannel(event: ChatModelChangeChannelEvent, chat: ChatNode) {
    logger.debug { "Changed channel: $event" }
  }
}
