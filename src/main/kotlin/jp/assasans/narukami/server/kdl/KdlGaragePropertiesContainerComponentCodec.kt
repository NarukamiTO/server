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
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.garage.item.GaragePropertiesContainerComponent
import jp.assasans.narukami.server.garage.item.IPropertyComponent

class KdlGaragePropertiesContainerComponentCodec : IKdlCodec<GaragePropertiesContainerComponent> {
  companion object {
    val Factory = object : IKdlCodecFactory<GaragePropertiesContainerComponent> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<GaragePropertiesContainerComponent>? {
        if(!type.kotlinClass.isSubclassOf(GaragePropertiesContainerComponent::class)) return null
        assert(!type.isMarkedNullable)

        return KdlGaragePropertiesContainerComponentCodec()
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  override fun decode(reader: KdlReader, node: KdlNode): GaragePropertiesContainerComponent {
    val components = node.children.associate { node ->
      logger.trace { "Decoding component: $node" }

      val clazz = Class.forName(node.name).kotlin
      logger.debug { "Loaded component class: $clazz" }

      if(!clazz.isSubclassOf(IPropertyComponent::class)) {
        throw IllegalArgumentException("Class $clazz is not a component")
      }
      @Suppress("UNCHECKED_CAST")
      clazz as KClass<out IPropertyComponent>

      @Suppress("UNCHECKED_CAST")
      val codec = reader.getCodec(clazz.createType()) as IKdlCodec<out IPropertyComponent>
      val component = codec.decode(reader, node)
      logger.debug { "Decoded component: $component" }

      clazz to component
    }

    return GaragePropertiesContainerComponent(components)
  }
}
