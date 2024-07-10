package io.kinference.primitives

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class PrimitivesCache : BuildService<BuildServiceParameters.None> {
    val sourceSetToResolved = mutableMapOf<String, Boolean>()
    val resolvedPaths = mutableSetOf<String>()
}
