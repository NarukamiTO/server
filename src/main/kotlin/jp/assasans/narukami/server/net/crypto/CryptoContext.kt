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

package jp.assasans.narukami.server.net.crypto

import io.netty.buffer.ByteBuf

/**
 * Official client uses two encryption modes (also called protection contexts).
 *
 * `XorCryptoContext` is not implemented due to it utter uselessness, it is not suitable
 * for any real encryption and was probably used to obfuscate the data a bit.
 * It does not use asymmetric cryptography, so you can decrypt the data by having packet dump.
 * The encryptor state is 8 bytes, exactly the same as the known method IDs, so you theoretically
 * can brute-force it even without the full packet dump.
 */
interface CryptoContext {
  fun encrypt(data: ByteBuf): ByteBuf
  fun decrypt(data: ByteBuf): ByteBuf
}
