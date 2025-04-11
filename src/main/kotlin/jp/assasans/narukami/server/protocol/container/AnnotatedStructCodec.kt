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

package jp.assasans.narukami.server.protocol.container

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.extensions.hasInheritedAnnotation
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.net.command.ProtocolPreserveOrder
import jp.assasans.narukami.server.protocol.Codec
import jp.assasans.narukami.server.protocol.ICodec
import jp.assasans.narukami.server.protocol.ProtocolBuffer

class AnnotatedStructCodec<T>(private val type: KType) : Codec<T>() {
  private val logger = KotlinLogging.logger { }

  private val preserveOrder = type.kotlinClass.hasInheritedAnnotation<ProtocolPreserveOrder>()

  private val fields: List<KProperty1<out Any, *>>
  private val constructor: KFunction<Any>
  private val parameters: List<KParameter>

  init {
    logger.debug { "Initializing codec for struct $type, preserve order: $preserveOrder" }
    this.constructor = type.kotlinClass.constructors.single()

    val parameters = constructor.parameters.toMutableList()
    if(!preserveOrder) {
      parameters.sortBy { it.name }
    }
    this.parameters = parameters.toList()

    val fieldsByName = type.kotlinClass.memberProperties.associateBy { it.name }
    this.fields = parameters.map { parameter -> requireNotNull(fieldsByName[parameter.name]) }

    logger.trace { "Encode: ${fields.map { it.name }}" }
    logger.trace { "Decode: ${parameters.map { it.name }}" }
  }

  override fun encode(buffer: ProtocolBuffer, value: T) {
    try {
      for(field in fields) {
        try {
          val fieldValue = field.getter.call(value)
          val codec = protocol.getCodec(field.returnType) as ICodec<Any?>
          codec.encode(buffer, fieldValue)
        } catch(exception: Exception) {
          throw IllegalArgumentException("Failed to encode field ${field.name}: ${field.returnType}", exception)
        }
      }
    } catch(exception: Exception) {
      throw IllegalArgumentException("Failed to encode struct $type", exception)
    }
  }

  override fun decode(buffer: ProtocolBuffer): T {
    try {
      val args = parameters.associate { parameter ->
        try {
          val codec = protocol.getCodec(parameter.type) as ICodec<Any?>
          logger.debug { "Decoding field ${parameter.name}: ${parameter.type} with codec $codec" }

          Pair(parameter, codec.decode(buffer))
        } catch(exception: Exception) {
          throw IllegalArgumentException("Failed to decode field ${parameter.name}: ${parameter.type}", exception)
        }
      }

      return constructor.callBy(args) as T
    } catch(exception: Exception) {
      throw IllegalArgumentException("Failed to decode struct $type", exception)
    }
  }
}
