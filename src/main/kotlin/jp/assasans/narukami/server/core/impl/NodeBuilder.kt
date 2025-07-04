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

import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import jp.assasans.narukami.server.core.*
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
      throw IllegalArgumentException("Parameter ${parameter.name}: ${parameter.type} is neither IModelConstructor nor IComponent")
    }

    @Suppress("UNCHECKED_CAST")
    return clazz as KClass<out IDataUnit>
  }

  private fun getComponents(type: KType): List<ComponentDefinition> {
    val constructor = type.kotlinClass.primaryConstructor ?: throw IllegalArgumentException("$type has no primary constructor")
    val parameters = constructor.parameters

    val components = mutableListOf<ComponentDefinition>()
    for(parameter in parameters) {
      check(!parameter.isVararg) { "Parameter $parameter is marked as vararg" }
      check(!parameter.isOptional) { "Parameter $parameter is marked as optional" }
      check(parameter.type.arguments.isEmpty()) { "Parameter $parameter has type arguments" }

      val clazz = normalizeParameterType(parameter, type)
      components.add(ComponentDefinition(parameter, clazz))
    }

    return components
  }

  private fun getComponentsV2(type: KType): List<ComponentV2Definition> {
    val constructor = type.kotlinClass.primaryConstructor ?: throw IllegalArgumentException("$type has no primary constructor")
    val parameters = constructor.parameters

    val components = mutableListOf<ComponentV2Definition>()
    for(parameter in parameters) {
      check(!parameter.isVararg) { "Parameter $parameter is marked as vararg" }
      check(!parameter.isOptional) { "Parameter $parameter is marked as optional" }
      check(parameter.type.arguments.isEmpty()) { "Parameter $parameter has type arguments" }

      val clazz = normalizeParameterType(parameter, type)

      check(clazz.isSubclassOf(IComponent::class)) { "Parameter $parameter: $clazz is not a subclass of IComponent" }
      @Suppress("UNCHECKED_CAST")
      clazz as KClass<out IComponent>

      components.add(ComponentV2Definition(parameter, clazz))
    }

    return components
  }

  fun getNodeDefinition(clazz: KType): NodeDefinition {
    val root = Node::class
    check(clazz.kotlinClass.isSubclassOf(root)) { "Class $clazz is not a subclass of ${root.qualifiedName}" }

    val components = getComponents(clazz)
    return NodeDefinition(clazz, components)
  }

  fun getNodeV2Definition(type: KType): NodeV2Definition {
    val root = NodeV2::class
    check(type.kotlinClass.isSubclassOf(root)) { "Class $type is not a subclass of ${root.qualifiedName}" }

    val components = getComponentsV2(type)
    return NodeV2Definition(
      type,
      components,
      matchTemplate = type.kotlinClass.findAnnotation<MatchTemplate>()?.template,
    )
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

  /**
   * @return `null` if node was not built
   */
  fun tryBuildLazy(nodeDefinition: NodeDefinition, gameObject: IGameObject, context: IModelContext): Node? {
    val node = tryBuildUninitializedLazy(
      nodeDefinition,
      gameObject.models.mapValues { (_, model) ->
        { model.provide(gameObject, context) }
      },
      gameObject.allComponents
    )
    node?.init(context, gameObject)

    return node
  }

  fun tryBuildLazyStateless(nodeDefinition: NodeV2Definition, gameObject: IGameObject): NodeV2? {
    val node = tryBuildUninitializedLazyStateless(nodeDefinition, gameObject.allComponents)
    node?.init(gameObject)

    return node
  }

  private fun tryBuildUninitializedLazy(
    nodeDefinition: NodeDefinition,
    models: Map<KClass<out IModelConstructor>, () -> IModelConstructor>,
    components: Map<KClass<out IComponent>, IComponent>
  ): Node? {
    logger.trace { "Trying to build node $nodeDefinition" }
    val constructor = nodeDefinition.type.kotlinClass.primaryConstructor ?: return null
    val parameters = constructor.parameters

    val args = mutableMapOf<KParameter, IDataUnit?>()
    for(parameter in parameters) {
      val type = normalizeParameterType(parameter, nodeDefinition.type)
      if(type.isSubclassOf(IModelConstructor::class)) {
        val provider = models[type]
        if(provider == null) {
          logger.trace { "Model $type not found" }
          return null
        }

        try {
          args[parameter] = provider()
        } catch(exception: Exception) {
          throw IllegalStateException("Failed to provide model $type", exception)
        }
      } else if(type.isSubclassOf(IComponent::class)) {
        val component = components[type]
        args[parameter] = if(component != null) {
          component
        } else {
          if(!parameter.type.isMarkedNullable) {
            logger.trace { "Component $type not found" }
            return null
          }
          null
        }
      } else {
        throw IllegalArgumentException("Parameter $parameter is neither IModelConstructor nor IComponent")
      }
    }

    return constructor.callBy(args) as Node
  }

  private fun tryBuildUninitializedLazyStateless(
    nodeDefinition: NodeV2Definition,
    components: Map<KClass<out IComponent>, IComponent>
  ): NodeV2? {
    logger.trace { "Trying to build node $nodeDefinition" }
    val constructor = nodeDefinition.type.kotlinClass.primaryConstructor ?: return null
    @Suppress("UNCHECKED_CAST")
    constructor as KFunction<NodeV2>

    val parameters = constructor.parameters

    val args = mutableMapOf<KParameter, IDataUnit?>()
    for(parameter in parameters) {
      val type = normalizeParameterType(parameter, nodeDefinition.type)
      if(type.isSubclassOf(IComponent::class)) {
        val component = components[type]
        args[parameter] = if(component != null) {
          component
        } else {
          if(!parameter.type.isMarkedNullable) {
            logger.trace { "Component $type not found" }
            return null
          }
          null
        }
      } else {
        throw IllegalArgumentException("Parameter $parameter is not a component")
      }
    }

    return constructor.callBy(args)
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

data class NodeV2Definition(
  val type: KType,
  val components: List<ComponentV2Definition>,
  val matchTemplate: KClass<out TemplateV2>?,
) {
  override fun toString() = "$type"
}

data class ComponentV2Definition(
  val parameter: KParameter,
  val type: KClass<out IComponent>,
)
