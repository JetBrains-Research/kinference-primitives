package io.kinference.primitives.utils.psi

import io.kinference.primitives.types.DataType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import kotlin.reflect.KProperty

fun KtAnnotationEntry.getDescriptor(context: BindingContext) = context[BindingContext.ANNOTATION, this]!!.forced()

inline fun <reified T : Annotation> KtAnnotationEntry.isAnnotation(context: BindingContext): Boolean {
    return getDescriptor(context).fqName?.asString() == T::class.qualifiedName
}

inline fun <reified T : Annotation> DeclarationDescriptor.isAnnotatedWith(): Boolean {
    return annotations.any { it.fqName?.asString() == T::class.qualifiedName }
}

inline fun <reified T : Annotation> KtAnnotated.isAnnotatedWith(context: BindingContext): Boolean {
    return annotationEntries.any { it.isAnnotation<T>(context) }
}

inline fun <reified T : Annotation> KtAnnotated.getAnnotation(context: BindingContext): KtAnnotationEntry {
    return annotationEntries.single { it.isAnnotation<T>(context) }
}

inline fun <reified T : Annotation> KtAnnotated.getAnnotationOrNull(context: BindingContext): KtAnnotationEntry? {
    return annotationEntries.singleOrNull { it.isAnnotation<T>(context) }
}


inline fun <reified T> KtAnnotationEntry.getValue(context: BindingContext, param: KProperty<T>): T? = getDescriptor(context).getValue(param)

inline fun <reified T> KtAnnotationEntry.getValue(context: BindingContext, param: String): T? = getDescriptor(context).getValue(param)

inline fun <reified T: Enum<T>> KtAnnotationEntry.getEnumValueArray(context: BindingContext, param: String, valueOf: (String) -> T): List<T> {
    val values = this.getValue<List<EnumValue>>(context, param) ?: emptyList()
    return values.map { valueOf(it.enumEntryName.asString()) }
}

inline fun <reified T> AnnotationDescriptor.getValue(param: KProperty<T>): T? = getValue(param.name)
inline fun <reified T> AnnotationDescriptor.getValue(param: String): T? = argumentValue(param)?.value as T?
