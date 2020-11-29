package io.kinference.primitives.utils.psi

import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil

val KtNamedDeclaration.qualifiedName
    get() = fqName?.asString() ?: error("FqName not found")

fun <T> T.forced(): T = ForceResolveUtil.forceResolveAllContents(this)
