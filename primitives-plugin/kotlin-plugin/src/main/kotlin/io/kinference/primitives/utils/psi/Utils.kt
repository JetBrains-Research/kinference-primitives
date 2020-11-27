package io.kinference.primitives.utils.psi

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import kotlin.reflect.KProperty

val KtNamedDeclaration.qualifiedName
    get() = fqName?.asString() ?: error("FqName not found")

fun <T> T.forced(): T = ForceResolveUtil.forceResolveAllContents(this)

fun KtAnnotationEntry.getDescriptor(context: BindingContext) = context[BindingContext.ANNOTATION, this]!!.forced()

inline fun <reified T : Annotation> KtAnnotationEntry.isAnnotation(context: BindingContext): Boolean {
    return getDescriptor(context).fqName?.asString() == T::class.qualifiedName
}

inline fun <reified T : Annotation> KtAnnotated.isAnnotatedWith(context: BindingContext): Boolean {

    return annotationEntries.any { it.isAnnotation<T>(context) }
}

inline fun <reified T> KtAnnotationEntry.getValue(context: BindingContext, param: KProperty<T>): T? = getDescriptor(context).getValue(param)
inline fun <reified T> KtAnnotationEntry.getValue(context: BindingContext, param: String): T? = getDescriptor(context).getValue(param)
inline fun <reified T> AnnotationDescriptor.getValue(param: KProperty<T>): T? = getValue(param.name)
inline fun <reified T> AnnotationDescriptor.getValue(param: String): T? = argumentValue(param)?.value as T?

fun <T> crossProduct(vararg collections: Collection<T>): List<List<T>> {
    if (collections.isEmpty()) return emptyList()

    val entries = collections.filter(Collection<T>::isNotEmpty)
    return entries.drop(1).fold(entries.first().map(::listOf)) { acc, entry ->
        acc.flatMap { list -> entry.map(list::plus) }
    }
}
