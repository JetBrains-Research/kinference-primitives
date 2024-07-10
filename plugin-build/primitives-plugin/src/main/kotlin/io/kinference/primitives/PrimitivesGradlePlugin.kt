package io.kinference.primitives

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

class PrimitivesGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val kotlinExt = project.extensions.findByType(KotlinProjectExtension::class.java) ?: return

        val primitivesExt = project.extensions.create(extensionName, PrimitivesExtension::class.java)

        val primitivesTask = project.tasks.register(primitivesTaskName, PrimitivesTask::class.java) { primitivesTask ->
            primitivesTask.generationPath.set(primitivesExt.generationPath)
        }

        project.afterEvaluate {
            val buildDependenciesTasks = project.tasks.named("buildNeeded").get().dependsOn.filterNot { it == "build" }.filterNotNull()
            for (task in buildDependenciesTasks) {
                primitivesTask.get().dependsOn(task)
            }
        }

        kotlinExt.sourceSets.all { sourceSet ->
            val sourceSetName = sourceSet.name
            val fullPath = primitivesExt.generationPath.dir(sourceSetName)

            sourceSet.kotlin.srcDir(fullPath)

            //Support for Incremental compilation
            primitivesTask.get().inputs
                .files(sourceSet.kotlin.asFileTree)
                .withPathSensitivity(PathSensitivity.ABSOLUTE)
                .normalizeLineEndings()
                .skipWhenEmpty()
        }

        fun configureCompilation(compilation: KotlinCompilation<*>) {
            val targetTask = compilation.compileTaskProvider

            targetTask.configure {
                it.dependsOn(primitivesTask)
            }
        }

        if (kotlinExt is KotlinMultiplatformExtension) {
            kotlinExt.targets.all { kotlinTarget ->
                kotlinTarget.compilations.all { compilation ->
                    configureCompilation(compilation)
                }
            }
        }

        if (kotlinExt is KotlinSingleTargetExtension<*>) {
            kotlinExt.target.compilations.all { compilation ->
                configureCompilation(compilation)
            }
        }
    }

    companion object {
        const val primitivesTaskName = "generatePrimitives"
        const val extensionName = "primitives"
    }
}
