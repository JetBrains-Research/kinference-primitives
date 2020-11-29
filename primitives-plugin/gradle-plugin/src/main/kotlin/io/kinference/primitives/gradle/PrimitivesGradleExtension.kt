package io.kinference.primitives.gradle

import org.gradle.api.Project

open class PrimitivesPluginExtension {
    var generationPath: String = "src/main/kotlin-gen"
    var incrementalCachePath: String? = null
}

internal val Project.primitives: PrimitivesPluginExtension
    get() = project.extensions.findByType(PrimitivesPluginExtension::class.java) ?: kotlin.run {
        extensions.create("primitives", PrimitivesPluginExtension::class.java)
    }

fun Project.primitives(configure: PrimitivesPluginExtension.() -> Unit) {
    primitives.apply(configure)
}
