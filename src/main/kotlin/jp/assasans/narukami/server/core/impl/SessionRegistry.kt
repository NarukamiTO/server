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

import jp.assasans.narukami.server.core.ISessionRegistry
import jp.assasans.narukami.server.net.session.ISession
import jp.assasans.narukami.server.net.session.SessionHash

class SessionRegistry : ISessionRegistry {
  private val sessions: MutableMap<SessionHash, ISession> = mutableMapOf()

  override val all: Set<ISession>
    get() = sessions.values.toSet()

  override fun add(value: ISession) {
    if(sessions.contains(value.hash)) {
      throw IllegalArgumentException("Session with hash ${value.hash} already exists")
    }

    sessions[value.hash] = value
  }

  override fun remove(value: ISession) {
    sessions.remove(value.hash)
  }

  override fun get(hash: SessionHash): ISession? {
    return sessions[hash]
  }

  override fun has(hash: SessionHash): Boolean {
    return sessions.contains(hash)
  }
}
