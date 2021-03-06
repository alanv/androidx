/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.processor.autovalue

import androidx.room.Ignore
import androidx.room.ext.asDeclaredType
import androidx.room.ext.asTypeElement
import androidx.room.ext.getAllMethods
import androidx.room.ext.getDeclaredMethods
import androidx.room.ext.getPackage
import androidx.room.ext.hasAnnotation
import androidx.room.ext.hasAnyOf
import androidx.room.ext.isAbstract
import androidx.room.ext.isPrivate
import androidx.room.ext.isStatic
import androidx.room.ext.isType
import androidx.room.ext.kindName
import androidx.room.ext.name
import androidx.room.ext.type
import androidx.room.ext.typeName
import androidx.room.processor.Context
import androidx.room.processor.PojoProcessor
import androidx.room.processor.PojoProcessor.Companion.TARGET_METHOD_ANNOTATIONS
import androidx.room.processor.ProcessorErrors
import androidx.room.vo.Constructor
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Field
import androidx.room.vo.Pojo
import androidx.room.vo.Warning
import com.google.auto.value.AutoValue.CopyAnnotations
import isSameType
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

/**
 * Delegate to process generated AutoValue class as a Pojo.
 */
class AutoValuePojoProcessorDelegate(
    private val context: Context,
    private val autoValueElement: TypeElement
) : PojoProcessor.Delegate {

    private val autoValueDeclaredType: DeclaredType by lazy {
        autoValueElement.asDeclaredType()
    }

    override fun onPreProcess(element: TypeElement) {
        val allMethods = autoValueElement.getAllMethods(context.processingEnv)
        val autoValueAbstractGetters = allMethods
            .filter { it.isAbstract() && it.parameters.size == 0 }

        // Warn about missing @AutoValue.CopyAnnotations in the property getters.
        autoValueAbstractGetters.forEach {
            val hasRoomAnnotation = it.annotationMirrors.map {
                it.annotationType.typeName().toString()
            }.any { it.contains("androidx.room") }
            if (hasRoomAnnotation && !it.hasAnnotation(CopyAnnotations::class)) {
                context.logger.w(Warning.MISSING_COPY_ANNOTATIONS, it,
                        ProcessorErrors.MISSING_COPY_ANNOTATIONS)
            }
        }

        // Check that certain Room annotations with @Target(METHOD) are not used in methods other
        // than the auto value abstract getters.
        (allMethods - autoValueAbstractGetters)
            .filter { it.hasAnyOf(*TARGET_METHOD_ANNOTATIONS) }
            .forEach { method ->
                val annotationName = TARGET_METHOD_ANNOTATIONS.first { method.hasAnnotation(it) }
                    .java.simpleName
                context.logger.e(method,
                    ProcessorErrors.invalidAnnotationTarget(annotationName, method.kindName()))
            }
    }

    override fun findConstructors(element: TypeElement): List<ExecutableElement> {
        val typeUtils = context.processingEnv.typeUtils
        return autoValueElement.getDeclaredMethods().filter {
            it.isStatic() &&
                    !it.hasAnnotation(Ignore::class) &&
                    !it.isPrivate() &&
                    it.returnType.isSameType(typeUtils, autoValueElement.type)
        }
    }

    override fun createPojo(
        element: TypeElement,
        declaredType: DeclaredType,
        fields: List<Field>,
        embeddedFields: List<EmbeddedField>,
        relations: List<androidx.room.vo.Relation>,
        constructor: Constructor?
    ): Pojo {
        return Pojo(element = element,
                type = autoValueDeclaredType,
                fields = fields,
                embeddedFields = embeddedFields,
                relations = relations,
                constructor = constructor)
    }

    companion object {
        /**
         * Gets the generated class name of an AutoValue annotated class.
         *
         * This is the same naming strategy used by AutoValue's processor.
         */
        fun getGeneratedClassName(element: TypeElement): String {
            var type = element
            var name = type.name
            while (type.enclosingElement.isType()) {
                type = type.enclosingElement.asTypeElement()
                name = "${type.name}_$name"
            }
            val pkg = type.getPackage()
            return "$pkg${if (pkg.isEmpty()) "" else "."}AutoValue_$name"
        }
    }
}