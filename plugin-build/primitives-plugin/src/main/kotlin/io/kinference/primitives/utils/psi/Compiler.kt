package io.kinference.primitives.utils.psi

import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil

internal fun <T: Any> T.forced(): T = ForceResolveUtil.forceResolveAllContents(this)
