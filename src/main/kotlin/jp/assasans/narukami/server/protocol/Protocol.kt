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

package jp.assasans.narukami.server.protocol

import kotlin.reflect.KType
import kotlin.reflect.full.createType
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.dispatcher.*
import jp.assasans.narukami.server.net.command.ControlCommand
import jp.assasans.narukami.server.net.command.ControlCommandCodec
import jp.assasans.narukami.server.net.command.HashRequestCommand
import jp.assasans.narukami.server.net.command.HashRequestCommandCodec
import jp.assasans.narukami.server.net.session.SessionHash
import jp.assasans.narukami.server.protocol.factory.*
import jp.assasans.narukami.server.protocol.primitive.*

interface IProtocol {
  val varIntCodec: ICodec<Int>

  fun getCodec(info: KType): ICodec<*>
}

inline fun <reified T> IProtocol.getTypedCodec(): ICodec<T> {
  return getCodec(T::class.createType()) as ICodec<T>
}

class Protocol : IProtocol {
  private val logger = KotlinLogging.logger { }

  private val codecs: MutableMap<KType, ICodec<*>> = mutableMapOf()
  private val factories: MutableList<ICodecFactory<*>> = mutableListOf()

  override val varIntCodec = VarIntCodec()

  init {
    factories.add(OptionalCodecFactory())
    factories.add(PairCodecFactory())
    factories.add(ListCodecFactory())
    factories.add(MapCodecFactory())
    factories.add(ResourceCodecFactory())
    factories.add(AnnotatedStructCodecFactory())
    factories.add(AnnotatedEnumCodecFactory())

    /* Primitives */
    register(Boolean::class.createType(), BooleanCodec())
    register(Byte::class.createType(), ByteCodec())
    register(Short::class.createType(), ShortCodec())
    register(Int::class.createType(), IntCodec())
    register(Long::class.createType(), LongCodec())
    register(Float::class.createType(), FloatCodec())
    register(Double::class.createType(), DoubleCodec())
    register(String::class.createType(), StringCodec())
    register(SessionHash::class.createType(), SessionHashCodec())
    register(IGameObject::class.createType(), GameObjectCodec())

    /* Control channel */
    register(ControlCommand::class.createType(), ControlCommandCodec())
    register(HashRequestCommand::class.createType(), HashRequestCommandCodec())

    /* Space channel */
    register(ModelData::class.createType(), ModelDataCodec())
    register(ObjectsData::class.createType(), ObjectsDataCodec())
    register(ObjectsDependencies::class.createType(), ObjectsDependenciesCodec())
  }

  fun register(info: KType, codec: ICodec<*>) {
    (codec as Codec<*>).init(this)
    codecs[info] = codec
  }

  override fun getCodec(info: KType): ICodec<*> {
    return codecs[info] ?: createCodec(info)
  }

  private fun createCodec(info: KType): ICodec<*> {
    val codec: ICodec<*>?

    // Try to create from factories
    for(factory in factories) {
      codec = factory.create(this@Protocol, info) ?: continue
      codec.init(this)
      logger.debug { "Created $codec with $factory for $info" }

      // TODO: Cache codec (by constructor arguments?)
      return codec
    }

    throw IllegalArgumentException("Codec for $info not found")
  }
}
