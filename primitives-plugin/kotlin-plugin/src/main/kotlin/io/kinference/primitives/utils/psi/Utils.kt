package io.kinference.primitives.utils.psi

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import kotlin.reflect.KClass

val KtNamedDeclaration.qualifiedName
    get() = fqName?.asString() ?: error("FqName not found")

val KClass<*>.fqName: FqName
    get() = FqName(this.qualifiedName!!)


fun <T> T.forced(): T = ForceResolveUtil.forceResolveAllContents(this)
