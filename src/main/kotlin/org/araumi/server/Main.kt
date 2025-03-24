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

package org.araumi.server

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.araumi.server.net.ConfigServer
import org.araumi.server.net.GameServer
import org.araumi.server.net.ResourceServer
import org.araumi.server.res.ResourceType
import org.araumi.server.res.ResourceTypeConverter
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger

fun provideObjectMapper(): ObjectMapper {
  val factory = JsonFactory.builder()
    .build()

  val prettyPrinter = DefaultPrettyPrinter()
  prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)

  val mapper = ObjectMapper(factory)
  mapper.registerModule(kotlinModule {
    withReflectionCacheSize(512)
    configure(KotlinFeature.NullToEmptyCollection, false)
    configure(KotlinFeature.NullToEmptyMap, false)
    configure(KotlinFeature.NullIsSameAsDefault, false)
    configure(KotlinFeature.SingletonSupport, false)
    configure(KotlinFeature.StrictNullChecks, false)
  })
  mapper.setDefaultPrettyPrinter(prettyPrinter)
  mapper.enable(SerializationFeature.INDENT_OUTPUT)

  mapper.registerModule(SimpleModule().apply {
    addSerializer(ResourceType::class.java, ResourceTypeConverter.Serializer)
    addDeserializer(ResourceType::class.java, ResourceTypeConverter.Deserializer)
  })

  return mapper
}

const val NOTICE = """Araumi TO is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Araumi TO is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Araumi TO.  If not, see <https://www.gnu.org/licenses/>."""

suspend fun main() {
  val logger = KotlinLogging.logger { }

  val color = System.getenv("NO_COLOR")?.takeIf { it.isNotBlank() } == null
  if(color) {
    System.err.println("\u001B[93m${NOTICE}\u001B[0m")
  } else {
    System.err.println(NOTICE)
  }

  initLogOverrides()

  val koin = startKoin {
    logger(SLF4JLogger())
    modules(module {
      single { provideObjectMapper() }

      singleOf(::ConfigServer)
      singleOf(::ResourceServer)
      singleOf(::GameServer)
    })
  }

  coroutineScope {
    launch { koin.koin.get<ConfigServer>().start() }
    launch { koin.koin.get<ResourceServer>().start() }
    launch { koin.koin.get<GameServer>().start() }
  }
}

fun initLogOverrides() {
  val directives = System.getenv("KOTLIN_LOG") ?: ""
  directives.split(",").forEach { directive ->
    if(directive.contains("=")) {
      val name = directive.substringBeforeLast('=')
      val level = Level.toLevel(directives.substringAfterLast('='))

      Configurator.setLevel(name, level)
      println("[initLogOverrides] Set logger $name level to $level")
    } else {
      val level = Level.toLevel(directive)

      Configurator.setRootLevel(level)
      println("[initLogOverrides] Set root logger level to $level")
    }
  }
}
