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
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import dev.kdl.KdlNode
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.TemplatedGameClass
import jp.assasans.narukami.server.core.impl.TransientGameObject
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.extensions.singleOrNullOrThrow

class KdlGameObjectCodec : IKdlCodec<IGameObject> {
  companion object {
    val Factory = object : IKdlCodecFactory<IGameObject> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<IGameObject>? {
        if(!type.kotlinClass.isSubclassOf(IGameObject::class)) return null
        assert(!type.isMarkedNullable)

        return KdlGameObjectCodec()
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  var name: String? = null

  override fun decode(reader: KdlReader, node: KdlNode): IGameObject {
    try {
      val id = TransientGameObject.stableId(requireNotNull(name) { "Game object name is not set" })

      val componentsNodes = node.children.singleOrNullOrThrow { it.name == "components" }?.children ?: emptyList()
      val components = componentsNodes.map { rootNode ->
        logger.trace { "Decoding component: $rootNode" }

        val clazz = Class.forName(rootNode.name).kotlin
        logger.debug { "Loaded component class: $clazz" }

        if(!clazz.isSubclassOf(IComponent::class)) {
          throw IllegalArgumentException("Class $clazz is not a component")
        }

        @Suppress("UNCHECKED_CAST")
        val codec = reader.getCodec(clazz.createType()) as IKdlCodec<out IComponent>
        val component = codec.decode(reader, rootNode)
        logger.debug { "Decoded component: $component" }

        component
      }.toSet()

      val templateNode = node.children.singleOrNullOrThrow { it.name == "template" }
      val gameObject = if(templateNode != null) {
        // Template V1
        val template = reader.getTypedCodec<ITemplate>().decode(reader, templateNode)

        @Suppress("UNCHECKED_CAST")
        val templateClass = template::class as KClass<ITemplate>

        val parent = TemplatedGameClass.fromTemplate(templateClass)
        TransientGameObject.instantiate(
          id,
          parent,
          template,
          components
        )
      } else {
        // Template V2
        val templateNode = node.children.single { it.name == "template2" }
        val template = reader.getTypedCodec<PersistentTemplateV2>().decode(reader, templateNode)

        val gameObject = template.instantiate(id)
        gameObject.addAllComponents(components)

        gameObject
      }
      logger.debug { "Decoded game object: $gameObject" }

      return gameObject
    } catch(exception: Exception) {
      throw IllegalArgumentException("Failed to decode game object $name", exception)
    }
  }
}
