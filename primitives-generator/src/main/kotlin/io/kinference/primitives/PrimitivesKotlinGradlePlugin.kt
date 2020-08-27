@file:Suppress("unused")

package io.kinference.primitives

import io.kinference.primitives.tasks.GenerateSources
import org.gradle.api.Plugin
import org.gradle.api.Project


class PrimitivesKotlinGradlePlugin : Plugin<Project> {
    @ExperimentalUnsignedTypes
    override fun apply(target: Project) {
        val task = target.tasks.create("generateSources", GenerateSources::class.java)

        target.afterEvaluate {
            if (primitives.generateOnImport) {
                task.act()
            }

            primitives.beforeTasks.forEach { target.tasks.getByName(it).dependsOn(task) }
        }
    }
}
