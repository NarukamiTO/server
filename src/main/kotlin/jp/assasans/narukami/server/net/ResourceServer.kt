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

package jp.assasans.narukami.server.net

import java.nio.file.Path
import java.nio.file.Paths
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import jp.assasans.narukami.server.res.Eager
import jp.assasans.narukami.server.res.LocalizationRes
import jp.assasans.narukami.server.res.RemoteGameResourceRepository
import jp.assasans.narukami.server.res.SwfRes

class ResourceServer(
  private val objectMapper: ObjectMapper,
  private val gameResourceManager: RemoteGameResourceRepository
) {
  private val logger = KotlinLogging.logger { }

  private lateinit var engine: EmbeddedServer<*, *>

  private val root: Path = Paths.get(requireNotNull(System.getProperty("res.root")) { "\"res.root\" system property is not set" })

  suspend fun start() {
    logger.info { "Starting resource server..." }
    engine = embeddedServer(Netty, port = 5192, host = "0.0.0.0") {
      install(CallLogging)
      install(DefaultHeaders) {
        header(HttpHeaders.Server, "Narukami TO, resource server, AGPLv3+")
      }
      install(StatusPages) {
        exception<Throwable> { call, exception ->
          logger.error(exception) { "An error occurred while processing ${call.request.uri}" }
        }
      }

      routing {
        get("crossdomain.xml") {
          call.respondText {
            buildString {
              appendLine("<?xml version=\"1.0\"?>")
              appendLine("<cross-domain-policy>")
              appendLine("<allow-access-from domain=\"*\" to-ports=\"*\"/>")
              appendLine("</cross-domain-policy>")
            }
          }
        }

        resource()

        route("resources") {
          resource()
        }

        get("index.json") {
          call.respondPath(root.resolve("00-resources.json"))
        }

        route("localization") {
          get("meta.json") {
            // get all LocalizationRes resources
            val localizations = gameResourceManager.getAll().filter { it.type == LocalizationRes }
            logger.info { "Found ${localizations.size} localization resources" }

            val files = localizations.associate { resource ->
              val resourceName = resource.name.substringAfterLast('.')
              val name = "${resourceName.uppercase()}.l18n"

              // Localization files are resolved rel
              val file = "../resources/${resource.id.encode()}/$resourceName.l18n"
              logger.debug { "$name -> $file" }

              Pair(name, file)
            }

            call.respondText { objectMapper.writeValueAsString(files) }
          }
        }

        route("libs") {
          get("manifest.json") {
            call.respondText {
              objectMapper.writeValueAsString(
                mapOf(
                  "entrance.swf" to "entrance-rolling-resv2-${Clock.System.now().epochSeconds}.swf",
                  "game.swf" to "game-rolling-resv2-${Clock.System.now().epochSeconds}.swf",
                  "software.swf" to "software-rolling-resv2-${Clock.System.now().epochSeconds}.swf",
                  "hardware.swf" to "hardware-rolling-resv2-${Clock.System.now().epochSeconds}.swf"
                )
              )
            }
          }

          get("{file}") {
            val file = requireNotNull(call.parameters["file"])
            val name = when {
              file.startsWith("Prelauncher")       -> "library.Prelauncher"
              file.startsWith("AlternativaLoader") -> "library.AlternativaLoader"
              file.startsWith("TanksErrorScreen")  -> "library.TanksErrorScreen"
              file.startsWith("software")          -> "library.a3d.software"
              file.startsWith("hardware")          -> "library.a3d.hardware"
              file.startsWith("entrance")          -> "library.entrance"
              file.startsWith("game")              -> "library.game"
              else                                 -> {
                call.respond(HttpStatusCode.NotFound, "No file $file found")
                return@get
              }
            }

            val resource = gameResourceManager.get(name, mapOf(), SwfRes, Eager)
            call.respondFile(root.resolve("${resource.id.encode()}/library.swf").toFile())
          }
        }
      }
    }.start(true)
  }

  private fun Route.resource() {
    get("{1}/{2}/{3}/{4}/{version}/{file}") {
      val params = call.parameters
      val path = listOf(
        params["1"],
        params["2"],
        params["3"],
        params["4"],
        params["version"],
        params["file"]
      ).requireNoNulls().joinToString("/")

      try {
        call.respondFile(root.resolve(path).toFile())
      } catch(exception: Exception) {
        logger.error(exception) { "Failed to get resource $path" }
      }
    }
  }
}
