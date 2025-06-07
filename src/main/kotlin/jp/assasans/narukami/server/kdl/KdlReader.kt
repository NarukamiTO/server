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

package jp.assasans.narukami.server.kdl

import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.jvmName
import dev.kdl.KdlDocument
import dev.kdl.KdlNode
import dev.kdl.KdlProperties
import dev.kdl.KdlProperty
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * KDL deserializer, converts DOM into objects.
 *
 * Format specification is available at [kdl.dev](https://kdl.dev).
 */
class KdlReader {
  private val logger = KotlinLogging.logger { }

  private val codecs: MutableMap<KType, IKdlCodec<*>> = mutableMapOf()
  private val factories: MutableList<IKdlCodecFactory<*>> = mutableListOf()

  init {
    /* Containers */
    factories.add(KdlListCodec.Factory)

    /* Primitives */
    factories.add(KdlNumberCodec.Factory)
    factories.add(KdlStringCodec.Factory)

    /* Custom types */
    factories.add(KdlResourceCodec.Factory)
    factories.add(KdlGameObjectCodec.Factory)
    factories.add(KdlTemplateV2Codec.Factory)
    factories.add(KdlTemplateCodec.Factory)

    /* Catch-all */
    factories.add(KdlEnumCodec.Factory)
    factories.add(KdlStructCodec.Factory)
  }

  fun getCodec(type: KType): IKdlCodec<*> {
    return codecs[type] ?: createCodec(type)
  }

  private fun createCodec(type: KType): IKdlCodec<*> {
    val codec: IKdlCodec<*>?
    for(factory in factories) {
      codec = factory.create(this, type) ?: continue
      logger.debug { "Created ${codec::class.qualifiedName} with ${factory::class.jvmName} for $type" }

      // TODO: Cache codec (by constructor arguments?)
      return codec
    }

    throw IllegalArgumentException("Codec for $type not found")
  }
}

inline fun <reified T> KdlReader.getTypedCodec(): IKdlCodec<T> {
  @Suppress("UNCHECKED_CAST")
  return getCodec(T::class.createType()) as IKdlCodec<T>
}

fun KdlDocument.asNode(): KdlNode {
  return KdlNode(
    null,
    "#root",
    emptyList(),
    KdlProperties.builder().build(),
    nodes
  )
}

fun KdlProperty<*>.asNode(): KdlNode {
  return KdlNode(
    null,
    this.name,
    listOf(this.value),
    KdlProperties.builder().build(),
    emptyList()
  )
}
