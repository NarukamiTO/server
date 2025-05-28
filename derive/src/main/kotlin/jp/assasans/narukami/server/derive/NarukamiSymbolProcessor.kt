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

package jp.assasans.narukami.server.derive

import java.io.OutputStream
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

class NarukamiSymbolProcessor(
  private val options: Map<String, String>,
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
  private operator fun OutputStream.plusAssign(value: String) {
    write(value.toByteArray())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation("jp.assasans.narukami.server.net.command.ProtocolClass")
      .filterIsInstance<KSClassDeclaration>()

    val templateTypeName = resolver.getKSNameFromString("jp.assasans.narukami.server.core.ITemplate")
    val templateType = resolver.getClassDeclarationByName(templateTypeName)
                       ?: throw IllegalStateException("Unable to find ${templateTypeName.asString()}")

    val componentTypeName = resolver.getKSNameFromString("jp.assasans.narukami.server.core.IComponent")
    val componentType = resolver.getClassDeclarationByName(componentTypeName)
                        ?: throw IllegalStateException("Unable to find ${componentTypeName.asString()}")

    // Exit from the processor in case nothing is annotated
    if(!symbols.iterator().hasNext()) return emptyList()

    // The generated file will be located at:
    // build/generated/ksp/main/kotlin/jp/assasans/narukami/server/derive/GeneratedFunctions.kt
    val file = codeGenerator.createNewFile(
      // Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
      // Learn more about incremental processing in KSP from the official docs:
      // https://kotlinlang.org/docs/ksp-incremental.html
      dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
      packageName = "jp.assasans.narukami.server.derive",
      fileName = "TemplateMapping"
    )

    file.use {
      file += "// This file is @generated. Do not edit.\n\n"

      file += "package jp.assasans.narukami.server.derive\n"
      file += "\n"
      file += "import kotlin.reflect.KClass\n"
      file += "import kotlin.reflect.KProperty1\n"
      file += "import jp.assasans.narukami.server.core.ITemplate\n"
      file += "import jp.assasans.narukami.server.core.internal.TemplateMember\n"
      file += "\n"
      file += "val templateToMembers: Map<KClass<out ITemplate>, Map<KProperty1<out ITemplate, *>, TemplateMember>> = mapOf(\n"

      symbols.forEach { it.accept(Visitor(file, templateType, componentType), Unit) }

      file += ")\n"
    }

    val unableToProcess = symbols.filterNot { it.validate() }.toList()
    return unableToProcess
  }

  inner class Visitor(
    private val file: OutputStream,
    private val templateType: KSClassDeclaration,
    private val componentType: KSClassDeclaration,
  ) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      if(classDeclaration.classKind != ClassKind.CLASS) {
        logger.error("Only classes can be annotated with @ProtocolClass", classDeclaration)
        return
      }

      val constructor = classDeclaration.primaryConstructor
      if(constructor == null) {
        logger.error("No primary constructor found for $classDeclaration", classDeclaration)
        return
      }

      val parameters = constructor.parameters.filter { it.validate() }

      file += "  ${classDeclaration.qualifiedName?.asString()}::class to mapOf(\n"

      parameters.forEach { parameter ->
        visitValueParameter(parameter, Unit)
      }

      file += "  ),\n"
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
      val clazz = valueParameter.parent?.parent as KSClassDeclaration

      val type = valueParameter.type.resolve()
      val typeName = type.declaration.qualifiedName?.asString()
      if(!typeName.equals("jp.assasans.narukami.server.core.IModelProvider")) {
        // Check for composite template
        if(templateType.asStarProjectedType().isAssignableFrom(type)) {
          file += "    ${clazz.qualifiedName?.asString()}::${valueParameter.name?.asString()} to TemplateMember.Template($typeName::class),\n"
        } else if(componentType.asStarProjectedType().isAssignableFrom(type)) {
          file += "    ${clazz.qualifiedName?.asString()}::${valueParameter.name?.asString()} to TemplateMember.Component($typeName::class),\n"
        } else {
          file += "    ${clazz.qualifiedName?.asString()}::${valueParameter.name?.asString()} to TemplateMember.Model($typeName::class),\n"
        }
        return
      }

      val typeArguments = type.arguments
      if(typeArguments.size != 1) {
        logger.error("IModelProvider must have exactly one type argument", valueParameter)
        return
      }

      val typeArgumentType = typeArguments[0].type
      if(typeArgumentType == null) {
        logger.error("IModelProvider has invalid type argument", valueParameter)
        return
      }

      val typeArgument = typeArgumentType.resolve()
      file += "    ${clazz.qualifiedName?.asString()}::${valueParameter.name?.asString()} to TemplateMember.Model(${typeArgument.declaration.qualifiedName?.asString()}::class), // extracted from model provider\n"
    }
  }
}
