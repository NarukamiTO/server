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

package jp.assasans.narukami.server.core

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import jp.assasans.narukami.server.core.internal.TemplateMember
import jp.assasans.narukami.server.net.command.ProtocolClass

/**
 * A template provides a set of models.
 */
interface ITemplate

@get:JvmName("KClass_IClass_protocolId")
val KClass<out ITemplate>.protocolId: Long
  get() = requireNotNull(findAnnotation<ProtocolClass>()) { "$this has no @ProtocolClass annotation" }.id

val KClass<out ITemplate>.models: Map<KProperty1<out ITemplate, *>, KClass<out IModelConstructor>>
  get() {
    val members = requireNotNull(jp.assasans.narukami.server.derive.templateToMembers[this]) {
      "$this is not registered"
    }

    return members.flatMap { (property, member) ->
      when(member) {
        is TemplateMember.Model    -> listOf(property to member.model)
        is TemplateMember.Template -> member.template.models.toList()
      }
    }.toMap()
  }
