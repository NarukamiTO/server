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
import org.koin.java.KoinJavaComponent.inject
import jp.assasans.narukami.server.NOTICE_SHORT
import jp.assasans.narukami.server.battlefield.UserGroupComponent
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.entrance.DispatcherNodeV2
import jp.assasans.narukami.server.lobby.CrystalsComponent
import jp.assasans.narukami.server.lobby.ScoreComponent
import jp.assasans.narukami.server.lobby.SessionLogoutEvent
import jp.assasans.narukami.server.lobby.UsernameComponent
import jp.assasans.narukami.server.lobby.user.UserTemplate
import jp.assasans.narukami.server.net.sessionNotNull

@MatchTemplate(CommunicationTemplate::class)
class ChatNodeV2 : NodeV2()

@MatchTemplate(UserTemplate::class)
data class UserNodeV2(
  val userGroup: UserGroupComponent,
  val username: UsernameComponent,
  val score: ScoreComponent,
  val crystals: CrystalsComponent,
) : NodeV2()

fun remote(space: ISpace, gameObject: IGameObject, allowUnloaded: Boolean = false): Set<IModelContext> {
  val sessions: ISessionRegistry by inject(ISessionRegistry::class.java)
  return sessions.all
    .mapNotNull { session -> session.spaces.get(space.id) }
    .filter { channel -> if(allowUnloaded) true else channel.loadedObjects.contains(gameObject.id) }
    .map { channel -> SpaceChannelModelContext(channel) }
    .toSet()
}

context(context: IModelContext)
fun remote(gameObject: IGameObject, allowUnloaded: Boolean = false): Set<IModelContext> {
  return remote(context.space, gameObject, allowUnloaded)
}

context(context: IModelContext)
fun remote(node: NodeV2, allowUnloaded: Boolean = false): Set<IModelContext> {
  return remote(context.space, node.gameObject, allowUnloaded)
}

class ChatSystem : AbstractSystem() {
  private val logger = KotlinLogging.logger { }

  @OnEventFireV2
  fun chatAdded(
    context: SpaceChannelModelContext,
    event: NodeAddedEvent,
    @Optional chat: ChatNodeV2,
  ) = context {
    val identity = context.channel.sessionNotNull.properties["identity"]?.split(",") ?: emptyList()
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
              if(identity.isNotEmpty()) appendLine()
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

  @OnEventFireV2
  fun sendMessage(
    context: IModelContext,
    event: ChatModelSendMessageEvent,
    chat: ChatNodeV2,
    user: UserNodeV2,
    @JoinAll dispatcher: DispatcherNodeV2,
  ) = context {
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
        userId = user.userGroup.key
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

    val chats = remote(chat)
    ChatModelShowMessagesEvent(messages = listOf(message)).schedule(chat, chats)
  }

  @OnEventFireV2
  fun changeChannel(
    context: IModelContext,
    event: ChatModelChangeChannelEvent,
    chat: ChatNodeV2,
  ) {
    logger.debug { "Changed channel: $event" }
  }
}
