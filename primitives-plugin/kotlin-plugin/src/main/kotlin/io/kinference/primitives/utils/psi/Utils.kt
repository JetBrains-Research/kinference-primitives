package io.kinference.primitives.utils.psi

import io.kinference.primitives.annotations.PrimitiveClass
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil

val KtNamedDeclaration.qualifiedName
    get() = fqName?.asString() ?: error("FqName not found")

fun <T> T.forced(): T = ForceResolveUtil.forceResolveAllContents(this)
