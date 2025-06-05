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

package jp.assasans.narukami.server.res

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.readValue
import dev.kdl.parse.KdlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.extensions.singleOrNullOrThrow
import jp.assasans.narukami.server.kdl.KdlGameObjectCodec
import jp.assasans.narukami.server.kdl.KdlReader
import jp.assasans.narukami.server.kdl.asNode
import jp.assasans.narukami.server.kdl.getTypedCodec

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceInfo(
  val name: String,
  val id: Long,
  val version: Long,
  val namespaces: Map<String, String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
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
  RawData(400, ""),
  Localization(-1, "Localization"), // Unused
  GameObject(-1, "GameObject");
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
    ResourceType.SwfLibrary        -> SwfRes
    ResourceType.Sound             -> SoundRes
    ResourceType.Map               -> MapRes
    ResourceType.Proplib           -> ProplibRes
    ResourceType.Texture           -> TextureRes
    ResourceType.Image             -> ImageRes
    ResourceType.MultiframeTexture -> MultiframeTextureRes
    ResourceType.LocalizedImage    -> LocalizedImageRes
    ResourceType.Object3D          -> Object3DRes
    ResourceType.Localization      -> LocalizationRes
    ResourceType.GameObject        -> GameObjectRes
    else                           -> throw IllegalArgumentException("Resource type $this is not supported")
  }
}

val Res.type: ResourceType
  get() = when(this) {
    is SwfRes               -> ResourceType.SwfLibrary
    is MapRes               -> ResourceType.Map
    is ProplibRes           -> ResourceType.Proplib
    is SoundRes             -> ResourceType.Sound
    is TextureRes           -> ResourceType.Texture
    is ImageRes             -> ResourceType.Image
    is MultiframeTextureRes -> ResourceType.MultiframeTexture
    is LocalizedImageRes    -> ResourceType.LocalizedImage
    is Object3DRes          -> ResourceType.Object3D
    is LocalizationRes      -> ResourceType.Localization
    is GameObjectRes        -> ResourceType.GameObject
    is UnknownRes           -> throw IllegalArgumentException("Resource type $this is not supported")
  }

/**
 * Represents a game resource.
 */
data class Resource<T : Res, L : Laziness>(
  val name: String,
  val namespaces: Map<String, String>,
  val id: ResourceId,
  val type: T,
  val laziness: L,
)

/**
 * Indicates whether the resource is lazily loaded or not.
 */
sealed interface Laziness {
  data object Undefined : Laziness
}

/**
 * Indicates that the resource is lazily loaded.
 */
data object Lazy : Laziness

/**
 * Indicates that the resource is eagerly loaded.
 */
data object Eager : Laziness

val Laziness.isLazy: Boolean
  get() = when(this) {
    Lazy               -> true
    Eager              -> false
    Laziness.Undefined -> throw IllegalStateException("Laziness is undefined")
  }

sealed interface Res
data object UnknownRes : Res
data object SoundRes : Res
data object SwfRes : Res
data object MapRes : Res
data object ProplibRes : Res
data object TextureRes : Res
data object ImageRes : Res
data object MultiframeTextureRes : Res
data object LocalizedImageRes : Res
data object Object3DRes : Res
data object LocalizationRes : Res
data object GameObjectRes : Res

interface IGameResourceRepository {
  fun getAll(): List<Resource<*, *>>
  fun <T : Res, L : Laziness> get(name: String, namespaces: Map<String, String>, type: T, laziness: L): Resource<T, L>
}

class RemoteGameResourceRepository(
  private val objectMapper: ObjectMapper,
  private val reader: KdlReader,
  private val spaces: IRegistry<ISpace>,
) : IGameResourceRepository {
  private val logger = KotlinLogging.logger { }

  // private val scope = CoroutineScope(coroutineContext + SupervisorJob())

  private val resources: MutableMap<String, Set<Resource<*, *>>> = mutableMapOf()

  private val root: Path = Paths.get(requireNotNull(System.getProperty("res.root")) { "\"res.root\" system property is not set" })

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
            resource.type.intoMarker(),
            Laziness.Undefined
          )
        }.toSet()
      }
    resources.putAll(newResources)

    logger.debug { "Fetched ${fetched.size} remote resources" }
  }

  fun createObjects() {
    for(resource in getAll().filter { it.type == GameObjectRes }) {
      val text = resolve(resource, "object.kdl").readText()
      val document = KdlParser.v2().parse(text)
      logger.trace { "KDL document: $document" }

      val codec = reader.getTypedCodec<IGameObject>() as KdlGameObjectCodec
      codec.name = "${resource.name}${resource.namespaces}"
      val gameObject = codec.decode(reader, document.asNode())

      val spaceNames = gameObject.getComponentOrNull<AutoAddedComponent>()?.spaces ?: emptyList()
      for(spaceName in spaceNames) {
        val space = spaces.get(Space.stableId(spaceName)) ?: throw IllegalArgumentException("Space $spaceName not found for $resource")
        logger.info { "Auto-adding ${codec.name} to $spaceName => $gameObject" }
        space.objects.add(gameObject)
      }
    }
  }

  override fun getAll(): List<Resource<*, *>> = resources.values.flatten().toList()

  override fun <T : Res, L : Laziness> get(name: String, namespaces: Map<String, String>, type: T, laziness: L): Resource<T, L> {
    logger.trace { "Get resource $name with namespaces $namespaces" }
    val resources = requireNotNull(resources[name]) { "Resource $name not found" }
    logger.trace { "Resources: $resources" }

    val resource = resources.singleOrNullOrThrow { resource -> resource.namespaces == namespaces }
                   ?: throw NoSuchElementException("Resource $name with namespaces $namespaces not found")

    check(resource.type == type) { "Resource $name is not of type $type" }

    val lazyResource = (resource as Resource<*, in Laziness>).copy(laziness = laziness)

    @Suppress("UNCHECKED_CAST")
    return lazyResource as Resource<T, L>
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

  // TODO: Should we abstract Path / File away and return ByteArray / String instead?
  fun resolve(resource: Resource<*, *>, file: String): Path {
    return root.resolve("${resource.id.encode()}/$file")
  }
}
