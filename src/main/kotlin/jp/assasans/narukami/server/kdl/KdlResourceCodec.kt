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
import kotlin.reflect.full.isSubclassOf
import dev.kdl.KdlNode
import dev.kdl.KdlProperty
import dev.kdl.KdlValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.narukami.server.extensions.kotlinClass
import jp.assasans.narukami.server.res.*

class KdlResourceCodec : IKdlCodec<Resource<*, *>>, KoinComponent {
  companion object {
    val Factory = object : IKdlCodecFactory<Resource<*, *>> {
      override fun create(reader: KdlReader, type: KType): IKdlCodec<Resource<*, *>>? {
        if(!type.kotlinClass.isSubclassOf(Resource::class)) return null
        assert(!type.isMarkedNullable)

        return KdlResourceCodec()
      }
    }
  }

  private val logger = KotlinLogging.logger { }

  private val gameResourceRepository: RemoteGameResourceRepository by inject()

  override fun decode(reader: KdlReader, node: KdlNode): Resource<*, *> {
    val name = (node.arguments.single() as KdlValue<String>).value()

    val namespaces = node.properties
      .filter { it.name.startsWith("@") }
      .associate { it.name.substring(1) to (it.value as KdlValue<String>).value() }

    val type = when(val value = (node.properties.single { it.name == "type" } as KdlProperty<String>).value.value()) {
      "SwfLibrary"        -> SwfRes
      "Sound"             -> SoundRes
      "Map"               -> MapRes
      "Proplib"           -> ProplibRes
      "Texture"           -> TextureRes
      "Image"             -> ImageRes
      "MultiframeTexture" -> MultiframeTextureRes
      "LocalizedImage"    -> LocalizedImageRes
      "Object3D"          -> Object3DRes
      "Localization"      -> LocalizationRes
      else                -> throw IllegalArgumentException("Invalid resource type: $value")
    }

    val laziness = when(val value = (node.properties.single { it.name == "laziness" } as KdlProperty<String>).value.value()) {
      "Eager" -> Eager
      "Lazy"  -> Lazy
      else    -> throw IllegalArgumentException("Invalid laziness: $value")
    }

    val resource = gameResourceRepository.get(
      name,
      namespaces,
      type,
      laziness
    )
    logger.debug { "Decoded resource: $resource" }

    return resource
  }
}
