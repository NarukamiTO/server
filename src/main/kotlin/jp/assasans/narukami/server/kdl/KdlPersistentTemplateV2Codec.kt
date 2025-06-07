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

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import dev.kdl.KdlNode
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.PersistentTemplateV2
import jp.assasans.narukami.server.extensions.kotlinClass

class KdlPersistentTemplateV2Codec : IKdlCodec<PersistentTemplateV2> {
  companion object {
    val Factory = object : IKdlCodecFactory<PersistentTemplateV2> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<PersistentTemplateV2>? {
        if(!type.kotlinClass.isSubclassOf(PersistentTemplateV2::class)) return null
        assert(!type.isMarkedNullable)

        return KdlPersistentTemplateV2Codec()
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  override fun decode(reader: KdlReader, node: KdlNode): PersistentTemplateV2 {
    val templateName = node.arguments.single().value() as String

    val clazz = Class.forName(templateName).kotlin
    if(!clazz.isSubclassOf(PersistentTemplateV2::class)) throw IllegalArgumentException("$clazz is not a template")
    @Suppress("UNCHECKED_CAST")
    clazz as KClass<out PersistentTemplateV2>

    logger.debug { "Loaded template class $clazz" }

    val template = requireNotNull(clazz.objectInstance) { "Template $clazz is not an object declaration" }
    logger.debug { "Template $template" }

    return template
  }
}
