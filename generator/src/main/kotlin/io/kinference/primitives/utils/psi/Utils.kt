package io.kinference.primitives.utils.psi

import io.kinference.primitives.utils.analysis.forced
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

val KtNamedDeclaration.qualifiedName
    get() = fqName?.asString() ?: error("FqName not found")

fun KtAnnotationEntry.getDescriptor(context: BindingContext) = context[BindingContext.ANNOTATION, this]!!.forced()

inline fun <reified T : Annotation> KtAnnotationEntry.isAnnotation(context: BindingContext): Boolean {
    return getDescriptor(context).fqName?.asString() == T::class.qualifiedName
}

inline fun <reified T : Annotation> KtAnnotated.isAnnotatedWith(context: BindingContext): Boolean {
    return annotationEntries.any { it.isAnnotation<T>(context) }
}