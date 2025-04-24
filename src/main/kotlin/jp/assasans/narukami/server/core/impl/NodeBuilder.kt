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

package jp.assasans.narukami.server.core.impl

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.IComponent
import jp.assasans.narukami.server.core.IDataUnit
import jp.assasans.narukami.server.core.IModelConstructor
import jp.assasans.narukami.server.core.Node
import jp.assasans.narukami.server.extensions.kotlinClass

class NodeBuilder {
  private val logger = KotlinLogging.logger { }

  fun normalizeParameterType(parameter: KParameter, type: KType): KClass<out IDataUnit> {
    val clazz: KClass<*>
    if(parameter.type.classifier is KTypeParameter) {
      val classifier = parameter.type.classifier as KTypeParameter
      logger.debug { "Type parameter: ${classifier::class.simpleName} $classifier" }

      val typeParameters = type.kotlinClass.typeParameters
      val typeArguments = typeParameters.indices.associate { index ->
        typeParameters[index].name to type.arguments[index]
      }
      logger.debug { "Type arguments: $typeArguments" }

      val argument = requireNotNull(typeArguments[classifier.name])
      logger.debug { "Type argument: $argument" }

      clazz = requireNotNull(argument.type).kotlinClass
      logger.debug { "Type argument class: $clazz" }
    } else {
      clazz = parameter.type.kotlinClass
    }

    if(!clazz.isSubclassOf(IModelConstructor::class) && !clazz.isSubclassOf(IComponent::class)) {
      throw IllegalArgumentException("Parameter $parameter is neither IModelConstructor nor IComponent")
    }

    @Suppress("UNCHECKED_CAST")
    return clazz as KClass<out IDataUnit>
  }

  private fun getComponents(type: KType): List<ComponentDefinition> {
    val constructor = type.kotlinClass.primaryConstructor ?: throw IllegalArgumentException("$type has no primary constructor")
    val parameters = constructor.parameters

    val components = mutableListOf<ComponentDefinition>()
    for(parameter in parameters) {
      check(!parameter.type.isMarkedNullable) { "Parameter $parameter is marked as nullable" }
      check(!parameter.isVararg) { "Parameter $parameter is marked as vararg" }
      check(!parameter.isOptional) { "Parameter $parameter is marked as optional" }
      check(parameter.type.arguments.isEmpty()) { "Parameter $parameter has type arguments" }

      val clazz = normalizeParameterType(parameter, type)
      components.add(ComponentDefinition(parameter, clazz))
    }

    return components
  }

  fun getNodeDefinition(clazz: KType): NodeDefinition {
    val root = Node::class
    check(clazz.kotlinClass.isSubclassOf(root)) { "Class $clazz is not a subclass of ${root.qualifiedName}" }

    val components = getComponents(clazz)
    return NodeDefinition(clazz, components)
  }

  fun collectNodeDefinitions(): List<NodeDefinition> {
    val nodeDefinitions = mutableListOf<NodeDefinition>()
    nodeDefinitions.add(getNodeDefinition(Node::class.createType()))

    ClassGraph().enableAllInfo().acceptPackages("jp.assasans.narukami").scan().use { scanResult ->
      @Suppress("UNCHECKED_CAST")
      val classes = scanResult
        .getSubclasses(Node::class.qualifiedName)
        .loadClasses()
        .map { it.kotlin }
        .filter { !it.isAbstract } as List<KClass<out Node>>
      logger.info { "Found ${classes.size} nodes" }

      for(clazz in classes) {
        logger.debug { "Discovered node: $clazz" }
        nodeDefinitions.add(getNodeDefinition(clazz.createType()))
      }
    }

    return nodeDefinitions
  }

  @Deprecated("Use tryBuildLazy instead", ReplaceWith("tryBuildLazy"))
  fun tryBuild(nodeDefinition: NodeDefinition, components: Set<IModelConstructor>): Node? {
    val constructor = nodeDefinition.type.kotlinClass.primaryConstructor ?: return null
    val parameters = constructor.parameters

    val args = mutableMapOf<KParameter, IModelConstructor>()
    for(parameter in parameters) {
      val type = normalizeParameterType(parameter, nodeDefinition.type)
      val component = components.firstOrNull { it::class == type }
      if(component == null) {
        logger.trace { "Component $type (for ${parameter.name}) not found in $components" }
        return null
      }

      args[parameter] = component
    }

    return constructor.callBy(args) as Node
  }

  fun tryBuildLazy(
    nodeDefinition: NodeDefinition,
    models: Map<KClass<out IModelConstructor>, () -> IModelConstructor>,
    components: Map<KClass<out IComponent>, IComponent>
  ): Node? {
    val constructor = nodeDefinition.type.kotlinClass.primaryConstructor ?: return null
    val parameters = constructor.parameters

    val args = mutableMapOf<KParameter, IDataUnit>()
    for(parameter in parameters) {
      val type = normalizeParameterType(parameter, nodeDefinition.type)
      val value = if(type.isSubclassOf(IModelConstructor::class)) {
        val provider = models[type]
        if(provider == null) {
          logger.trace { "Model $type not found" }
          return null
        }

        args[parameter] = provider()
      } else if(type.isSubclassOf(IComponent::class)) {
        val component = components[type]
        if(component == null) {
          logger.trace { "Component $type not found" }
          return null
        }

        args[parameter] = component
      } else {
        throw IllegalArgumentException("Parameter $parameter is neither IModelConstructor nor IComponent")
      }
    }

    return constructor.callBy(args) as Node
  }
}

data class NodeDefinition(
  val type: KType,
  val components: List<ComponentDefinition>
)

data class ComponentDefinition(
  val parameter: KParameter,
  val type: KClass<out IDataUnit>,
)
