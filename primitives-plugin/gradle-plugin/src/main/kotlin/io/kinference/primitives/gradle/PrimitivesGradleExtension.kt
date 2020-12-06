package io.kinference.primitives.gradle

import org.gradle.api.Project

/**
 * Primitives Generator plugin extension describes current configuration
 * of plugin.
 */
open class PrimitivesPluginExtension {
    /**
     * Path to which code should be generated.
     *
     * It would be automatically added to source set and marked
     * as generated in IntelliJ IDEA
     */
    var generationPath: String = "src/main/kotlin-gen"

    /**
     * Path to which plugin can save service information
     * about incremental compilation
     */
    var incrementalCachePath: String? = null
}

internal val Project.primitives: PrimitivesPluginExtension
    get() = project.extensions.findByType(PrimitivesPluginExtension::class.java) ?: kotlin.run {
        extensions.create("primitives", PrimitivesPluginExtension::class.java)
    }

/**
 * Primitives Generator configuration extension.
 */
fun Project.primitives(configure: PrimitivesPluginExtension.() -> Unit) {
    primitives.apply(configure)
}
