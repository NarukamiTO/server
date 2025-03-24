/*
 * Araumi TO - a server reimplementation for a certain browser tank game.
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

plugins {
  kotlin("jvm") version "2.1.10"
  application
  idea
}

group = "org.araumi"
version = "0.1.0"

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.netty:netty-all:4.1.119.Final")

  /* HTTP */
  implementation("io.ktor:ktor-server-core:3.1.1")
  implementation("io.ktor:ktor-server-netty:3.1.1")
  implementation("io.ktor:ktor-server-call-logging:3.1.1")
  implementation("io.ktor:ktor-server-default-headers:3.1.1")
  implementation("io.ktor:ktor-client-core:3.1.1")
  implementation("io.ktor:ktor-client-cio:3.1.1")
  implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
  implementation("io.ktor:ktor-serialization-jackson:3.1.1")

  /* Serialization */
  implementation("com.fasterxml.jackson.core:jackson-core:2.18.3")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("org.glassfish.jaxb:jaxb-runtime:2.3.1")

  /* Reflection */
  implementation("io.github.classgraph:classgraph:4.8.179")

  /* Dependency Injection */
  implementation("io.insert-koin:koin-core:4.0.2")
  implementation("io.insert-koin:koin-logger-slf4j:4.0.2")

  /* Logging */
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
  implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")
  implementation("org.apache.logging.log4j:log4j-api:2.24.3")
  implementation("org.apache.logging.log4j:log4j-core:2.24.3")
  implementation("org.apache.logging.log4j:log4j-layout-template-json:2.24.3")

  /* Kotlin Utilities */
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
  implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

  testImplementation(kotlin("test"))
}

configurations {
  create("runtime").extendsFrom(compileClasspath.get(), runtimeOnly.get())
}

tasks {
  val copyDependencies = register<Sync>("copyDependencies") {
    from(configurations.named("runtime"))
    into(layout.buildDirectory.dir("dependencies"))
  }

  val packageSources = register<Zip>("packageSources") {
    archiveFileName.set("sources.zip")
    destinationDirectory.set(layout.buildDirectory.dir("tmp"))

    from(sourceSets.main.get().kotlin)
  }

  processResources {
    dependsOn(packageSources)

    // Include sources in the JAR
    from(packageSources.get().archiveFile.get()) {
      into("META-INF/")
    }
  }

  jar {
    dependsOn(copyDependencies)
  }

  test {
    useJUnitPlatform()
  }
}

application {
  mainClass.set("org.araumi.server.MainKt")
}

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}
