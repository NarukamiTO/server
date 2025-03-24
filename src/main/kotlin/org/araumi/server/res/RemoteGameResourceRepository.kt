/*
 * Araumi TO - a server software reimplementation for a certain browser tank game.
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

package org.araumi.server.res

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import org.araumi.server.extensions.singleOrNullOrThrow

data class ResourceInfo(
  val name: String,
  val id: Long,
  val version: Long,
  val namespaces: Map<String, String>
)

data class PartialRemoteResource(
  val info: ResourceInfo,
  val type: ResourceType
)

enum class ResourceType(val id: Short, val type: String) {
  SwfLibrary(1, "SwfLibrary"), // Unused
  Sound(4, "Sound"),
  Map(7, "Map"),
  Proplib(8, "Proplib"),
  Texture(9, "Texture"),
  Image(10, "Image"),
  MultiframeTexture(11, "MultiframeTexture"),
  ScalableImage(12, ""),
  LocalizedImage(13, "LocalizedImage"),
  Object3D(17, "Object3D"),
  Effects(25, ""), // Unused
  RawData(400, ""); // Unused
}

object ResourceTypeConverter {
  object Deserializer : JsonDeserializer<ResourceType>() {
    private val cache = ResourceType.entries.associateBy { it.type }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): ResourceType {
      val type = parser.text
      return cache[type] ?: throw IllegalArgumentException("Unknown resource type: $type")
    }
  }

  object Serializer : JsonSerializer<ResourceType>() {
    override fun serialize(value: ResourceType, generator: JsonGenerator, provider: SerializerProvider) {
      generator.writeNumber(value.type)
    }
  }
}

// private fun <T : Resource> RemoteResource.into(): T {
//   return when(type) {
//     ResourceType.SwfLibrary        -> SwfLibraryResource(name, id, info)
//     ResourceType.Sound             -> SoundResource(name, id, info)
//     ResourceType.Map               -> MapResource(name, id, info)
//     ResourceType.Proplib           -> ProplibResource(name, id, info)
//     ResourceType.Texture           -> TextureResource(name, id, info)
//     ResourceType.Image             -> ImageResource(name, id, info)
//     ResourceType.MultiframeTexture -> MultiframeTextureResource(name, id, info)
//     ResourceType.LocalizedImage    -> LocalizedImageResource(name, id, info)
//     ResourceType.Object3D          -> Object3DResource(name, id, info)
//     else                           -> TODO("Not supported: $type")
//   }.also { it.meta = meta } as T
// }

private fun ResourceType.intoMarker(): Res {
  return when(this) {
    ResourceType.Map -> MapRes
    else             -> throw IllegalArgumentException("Resource type $this is not supported")
  }
}

class Resource<T : Res>(
  val name: String,
  val namespaces: Map<String, String>,
  val id: ResourceId,
  val type: T
)

interface Res
object MapRes : Res

data class ShitConstructor(
  val backgroundImage: Resource<MapRes>,
)

class RemoteGameResourceRepository(
  private val objectMapper: ObjectMapper
) {
  private val logger = KotlinLogging.logger { }

  private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
      jackson()
    }
  }

  // private val scope = CoroutineScope(coroutineContext + SupervisorJob())

  private val resources: MutableMap<String, Set<Resource<*>>> = mutableMapOf()

  private val root: Path = Paths.get(requireNotNull(System.getenv("RESOURCES_ROOT")) { "No \"RESOURCES_ROOT\" environment variable set" })

  // @OptIn(FlowPreview::class)
  // suspend fun init() {
  //   val watchService = root.watch(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
  //   scope.launch {
  //     watchService.eventFlow()
  //       .flatMapConcat { it.asFlow() }
  //       .filter { event ->
  //         val entry = event.context() as Path
  //         entry.name == "00-resources.json"
  //       }
  //       .debounce(1.seconds)
  //       .collect {
  //         logger.info { "Reloading remote resources due to index change..." }
  //         fetch()
  //       }
  //   }
  //
  //   logger.info { "Started resource watcher" }
  // }

  suspend fun fetch() {
    val text = root.resolve("00-resources.json").readText()
    val fetched = objectMapper.readValue<List<PartialRemoteResource>>(text)
    resources.clear()
    val newResources = fetched
      .groupBy { resource -> resource.info.name }
      .mapValues { (_, resources) ->
        resources.map { resource ->
          Resource(
            resource.info.name,
            resource.info.namespaces,
            ResourceId(resource.info.id, resource.info.version),
            resource.type.intoMarker()
          )
        }.toSet()
      }
    resources.putAll(newResources)

    logger.debug { "Fetched ${fetched.size} remote resources" }
  }

  fun getAll(): List<Resource<*>> = resources.values.flatten().toList()

  fun <T : Res> get(name: String, namespaces: Map<String, String>, type: T): Resource<T> {
    logger.trace { "Get resource $name with namespaces $namespaces" }
    val resources = requireNotNull(resources[name]) { "Resource $name not found" }
    logger.trace { "Resources: $resources" }

    val resource = resources.singleOrNullOrThrow { resource -> resource.namespaces == namespaces }
                   ?: throw NoSuchElementException("Resource $name with namespaces $namespaces not found")

    check(resource.type == type) { "Resource $name is not of type $type" }

    @Suppress("UNCHECKED_CAST")
    return resource as Resource<T>
  }

  // fun <T : Res> get(reference: ResourceReference<T>): Resource<T> {
  //   logger.trace { "Get resource $reference" }
  //   val resources = requireNotNull(resources[reference.name]) { "Resource ${reference.name} not found" }
  //   logger.trace { "Resources: $resources" }
  //
  //   val resource = resources.singleOrNullOrThrow { resource -> resource.info.namespaces == reference.namespaces }
  //                  ?: throw NoSuchElementException("Resource $reference not found")
  //   return resource.into()
  // }

  // fun <T : Res> getAll(reference: ResourceReference<T>): Set<Resource<T>> {
  //   logger.trace { "Get resource $reference" }
  //   return requireNotNull(resources[reference.name]) { "Resource $reference not found" }
  //     .filter { it.info.namespaces.entries.containsAll(reference.namespaces.entries) }
  //     .map { it.into<T>() }
  //     .toSet()
  // }
}
