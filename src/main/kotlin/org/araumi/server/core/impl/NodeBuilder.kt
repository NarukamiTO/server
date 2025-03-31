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

package org.araumi.server.core.impl

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import org.araumi.server.core.IModelConstructor
import org.araumi.server.core.Node
import org.araumi.server.extensions.kotlinClass

class NodeBuilder {
  private val logger = KotlinLogging.logger { }

  fun getComponents(clazz: KClass<out Node>): List<ComponentDefinition> {
    val constructor = clazz.primaryConstructor ?: throw IllegalArgumentException("Class $clazz has no primary constructor")
    val parameters = constructor.parameters
    for(parameter in parameters) {
      check(!parameter.type.isMarkedNullable) { "Parameter $parameter is marked as nullable" }
      check(!parameter.isVararg) { "Parameter $parameter is marked as vararg" }
      check(!parameter.isOptional) { "Parameter $parameter is marked as optional" }
      check(parameter.type.arguments.isEmpty()) { "Parameter $parameter has type arguments" }

      val root = IModelConstructor::class
      if(!parameter.type.kotlinClass.isSubclassOf(root)) {
        throw IllegalArgumentException("Parameter $parameter is not a subclass of ${root.qualifiedName}")
      }
    }

    return parameters.map { parameter ->
      @Suppress("UNCHECKED_CAST")
      val type = parameter.type.kotlinClass as KClass<out IModelConstructor>

      ComponentDefinition(parameter, type)
    }
  }

  fun getNodeDefinition(clazz: KClass<out Node>): NodeDefinition {
    val root = Node::class
    check(clazz.isSubclassOf(root)) { "Class $clazz is not a subclass of ${root.qualifiedName}" }

    val components = getComponents(clazz)
    return NodeDefinition(clazz, components)
  }

  fun collectNodeDefinitions(): List<NodeDefinition> {
    val nodeDefinitions = mutableListOf<NodeDefinition>()
    nodeDefinitions.add(getNodeDefinition(Node::class))

    ClassGraph().enableAllInfo().acceptPackages("org.araumi").scan().use { scanResult ->
      @Suppress("UNCHECKED_CAST")
      val classes = scanResult
        .getSubclasses(Node::class.qualifiedName)
        .loadClasses()
        .map { it.kotlin } as List<KClass<out Node>>
      logger.info { "Found ${classes.size} nodes" }

      for(clazz in classes) {
        logger.debug { "Discovered node: $clazz" }
        nodeDefinitions.add(getNodeDefinition(clazz))
      }
    }

    return nodeDefinitions
  }

  fun tryBuild(nodeDefinition: NodeDefinition, components: Set<IModelConstructor>): Node? {
    val constructor = nodeDefinition.clazz.primaryConstructor ?: return null
    val parameters = constructor.parameters

    val args = mutableMapOf<KParameter, IModelConstructor>()
    for(parameter in parameters) {
      val component = components.firstOrNull { it::class == parameter.type.kotlinClass }
      if(component == null) {
        logger.warn { "Component $parameter not found" }
        return null
      }

      args[parameter] = component
    }

    return constructor.callBy(args)
  }
}

data class NodeDefinition(
  val clazz: KClass<out Node>,
  val components: List<ComponentDefinition>
)

data class ComponentDefinition(
  val parameter: KParameter,
  val type: KClass<out IModelConstructor>,
)
