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

import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import dev.kdl.KdlNode
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.ITemplate
import jp.assasans.narukami.server.core.ITemplateProvider
import jp.assasans.narukami.server.extensions.kotlinClass

class KdlTemplateCodec : IKdlCodec<ITemplate> {
  companion object {
    val Factory = object : IKdlCodecFactory<ITemplate> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<ITemplate>? {
        if(!type.kotlinClass.isSubclassOf(ITemplate::class)) return null
        assert(!type.isMarkedNullable)

        return KdlTemplateCodec()
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  override fun decode(reader: KdlReader, node: KdlNode): ITemplate {
    val templateName = node.arguments.single().value() as String
    val template = if(templateName.contains("::")) {
      val parts = templateName.split("::", limit = 2)
      val className = parts[0]
      val propertyName = parts[1]
      logger.debug { "Template class $className, provider $propertyName" }

      val clazz = Class.forName(className).kotlin
      logger.debug { "Loaded template class $clazz" }

      val companionClass = requireNotNull(clazz.companionObject)
      val companionInstance = requireNotNull(clazz.companionObjectInstance)

      val property = companionClass.declaredMemberProperties.single { it.name == propertyName }
      @Suppress("UNCHECKED_CAST")
      property as KProperty1<Any, ITemplateProvider<*>>

      val provider = property.get(companionInstance)
      provider.create()
    } else TODO()
    logger.debug { "Template $template" }

    return template
  }
}
