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

package org.araumi.server.extensions

import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

fun KClass<*>.findInheritedAnnotation(type: KClass<out Annotation>): Annotation? {
  var currentClass: KClass<*>? = this
  while(currentClass != null) {
    val annotation = currentClass.annotations.find { it.annotationClass == type }
    if(annotation != null) return annotation

    currentClass = currentClass.superclasses.firstOrNull()
  }

  return null
}

inline fun <reified T : Annotation> KClass<*>.findInheritedAnnotation(): T? {
  return findInheritedAnnotation(T::class) as T?
}

fun KClass<*>.hasInheritedAnnotation(type: KClass<out Annotation>): Boolean {
  return findInheritedAnnotation(type) != null
}

inline fun <reified T : Annotation> KClass<*>.hasInheritedAnnotation(): Boolean {
  return hasInheritedAnnotation(T::class)
}
