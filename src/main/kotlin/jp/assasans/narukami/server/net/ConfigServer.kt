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

import java.io.ByteArrayOutputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class ConfigServer {
  private val logger = KotlinLogging.logger { }

  private lateinit var engine: EmbeddedServer<*, *>

  suspend fun start() {
    logger.info { "Starting config server..." }
    engine = embeddedServer(Netty, port = 5191, host = "0.0.0.0") {
      install(CallLogging)
      install(DefaultHeaders) {
        header(HttpHeaders.Server, "Narukami TO, config server, AGPLv3+")
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

        /**
         * Used by the official launcher for load balancing purposes.
         *
         * This endpoint is shared between all nodes.
         */
        get("/s/status.js") {
          call.respond(
            Status(
              nodes = mapOf(
                "main.c1" to StatusNode(
                  endpoint = NodeEndpoint(
                    host = "127.0.0.1",
                    status = "NORMAL",
                    tcpPorts = listOf(5190),
                    wsPorts = listOf()
                  )
                )
              )
            )
          )
        }

        /**
         * Used by the game client to get server's endpoints.
         */
        get("config.xml") {
          call.respondText(ContentType.Application.Xml) {
            val stream = ByteArrayOutputStream()
            JAXBContext.newInstance(NodeConfig::class.java).createMarshaller().marshal(
              NodeConfig(
                server = NodeConfig.Server(
                  address = "127.0.0.1",
                  tcpPorts = listOf(
                    NodeConfig.Server.Port(5190)
                  ),
                  webSocketPorts = listOf()
                )
              ),
              stream
            )
            stream.toByteArray().decodeToString()
          }
        }

        get("sources.jar") {
          val resource = this::class.java.classLoader.getResource("sources.jar")
          if(resource == null) {
            call.respond(HttpStatusCode.NotFound, "sources.jar not found in the JAR")
            return@get
          }

          val inputStream = resource.openStream()
          call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"sources.jar\"")
          call.respondOutputStream(ContentType(ContentType.Application.TYPE, "java-archive")) {
            inputStream.copyTo(this)
          }
        }
      }
    }.start(true)
  }
}

data class Status(
  val linkForDownloadAPK: String = "",
  val maxSupportedAndroidVersion: Int = 2147483647,
  val minSupportedAndroidVersion: Int = 1661932145,
  val nodes: Map<String, StatusNode>
)

data class StatusNode(
  val endpoint: NodeEndpoint,
  @JsonProperty("inbattles") val inBattles: Int = 0,
  val online: Int = 1,
  val partners: Map<String, Int> = mapOf()
)

data class NodeEndpoint(
  val host: String,
  val status: String,
  val tcpPorts: List<Int>,
  val wsPorts: List<Int>
)

@XmlType
@XmlRootElement(name = "cfg")
@XmlAccessorType(XmlAccessType.FIELD)
class NodeConfig private constructor() {
  @field:XmlAttribute val xmlns: String = "http://alternativaplatform.com/core/config.xsd"

  @field:XmlElement(required = true) lateinit var server: Server
  @field:XmlElement(required = true) lateinit var status: String

  constructor(server: Server, status: String = "normal") : this() {
    this.server = server
    this.status = status
  }

  @XmlType
  @XmlAccessorType(XmlAccessType.FIELD)
  data class Server(
    @field:XmlAttribute(required = true) val address: String,
    @field:XmlAttribute(required = true) val mode: String = "simple",
    @field:XmlElementWrapper(name = "ports", required = true)
    @field:XmlElement(name = "port") val tcpPorts: List<Port>,
    @field:XmlElementWrapper(required = true)
    @field:XmlElement(name = "port") val webSocketPorts: List<Port>
  ) {
    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    data class Port(
      @field:XmlValue val port: Int
    )
  }
}
