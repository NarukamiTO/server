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
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import dev.kdl.parse.KdlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.IGameObject
import jp.assasans.narukami.server.core.IRegistry
import jp.assasans.narukami.server.core.ISpace
import jp.assasans.narukami.server.core.impl.Space
import jp.assasans.narukami.server.kdl.KdlGameObjectCodec
import jp.assasans.narukami.server.kdl.KdlReader
import jp.assasans.narukami.server.kdl.asNode
import jp.assasans.narukami.server.kdl.getTypedCodec

private fun discoverResourcesRoot(): Path {
  var directory = Paths.get("data")

  // Gradle distribution / Gradle JAR
  if(!directory.exists()) directory = Paths.get("../data")

  // IntelliJ IDEA, default working directory
  if(!directory.exists()) directory = Paths.get("src/main/resources/data")

  // IntelliJ IDEA, 1 level deep working directory
  if(!directory.exists()) directory = Paths.get("../src/main/resources/data")

  if(!directory.exists()) throw Exception("Cannot find runtime resources directory")
  return directory
}

class LocalGameResourceRepository(
  private val reader: KdlReader,
  private val spaces: IRegistry<ISpace>,
) : IGameResourceRepository {
  private val logger = KotlinLogging.logger { }

  private val root = discoverResourcesRoot()

  suspend fun fetch() {
    logger.info { "Loading local resources from $root" }

    root.toFile().walk().forEach { file ->
      if(!file.isFile) return@forEach
      if(file.extension != "kdl") return@forEach
      logger.trace { "Reading $file" }

      val path = file.toPath()
      val rawName = path
        .relativeTo(root)
        .normalize()
        .pathString
        .substringBeforeLast(".")
        .replace(path.fileSystem.separator, ".")

      val components = rawName.split('.')

      val name = components
        .filter { !it.startsWith('@') }
        .joinToString(".")
      val namespaces = components
        .filter { it.startsWith('@') }
        .map { it.substring(1).split('=', limit = 2) }
        .associate { Pair(it[0], it[1]) }

      val content = file.readText()
      val document = KdlParser.v2().parse(content)
      logger.trace { "KDL document: $document" }

      val codec = reader.getTypedCodec<IGameObject>()
      // TODO: Workaround, works for now
      (codec as KdlGameObjectCodec).name = name
      val gameObject = codec.decode(reader, document.asNode())

      val spaceName = namespaces["space"] ?: throw IllegalArgumentException("No space assigned for $file")
      val space = spaces.get(Space.stableId(spaceName)) ?: throw IllegalArgumentException("Space $spaceName not found for $file")

      logger.info { "Adding $name to $spaceName => $gameObject" }
      space.objects.add(gameObject)
    }
  }

  override fun getAll(): List<Resource<*, *>> {
    TODO("Not yet implemented")
  }

  override fun <T : Res, L : Laziness> get(name: String, namespaces: Map<String, String>, type: T, laziness: L): Resource<T, L> {
    TODO("Not yet implemented")
  }
}
