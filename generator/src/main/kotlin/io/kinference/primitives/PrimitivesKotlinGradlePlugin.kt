package io.kinference.primitives

import io.kinference.primitives.tasks.GenerateSources
import org.gradle.api.Plugin
import org.gradle.api.Project


open class PrimitivesPluginExtension {
    var generationPath: String = "src/main/kotlin-gen"
}

@Suppress("unused")
class PrimitivesKotlinGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val ext = target.extensions.create("primitives", PrimitivesPluginExtension::class.java)

        val task = target.tasks.create("generateSources", GenerateSources::class.java)
        target.tasks.getByName("classes").dependsOn.add(task)
    }
}
