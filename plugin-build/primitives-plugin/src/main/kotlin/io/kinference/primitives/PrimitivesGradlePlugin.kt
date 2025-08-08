package io.kinference.primitives

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool

class PrimitivesGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val kotlinExt = project.extensions.findByType(KotlinProjectExtension::class.java) ?: return

        val primitivesExt = project.extensions.create(extensionName, PrimitivesExtension::class.java)

        val primitivesCache = project.gradle.sharedServices.registerIfAbsent("${project.path}_${primitivesCacheName}", PrimitivesCache::class.java) {
            it.maxParallelUsages.set(1)
        }

        val generalPrimitivesTask = project.tasks.register(primitivesTaskName) {
            it.group = "generate"
        }


        kotlinExt.sourceSets.all { sourceSet ->
            sourceSet.kotlin.srcDir(primitivesExt.generationPath.dir(sourceSet.name))
            primitivesCache.get().sourceSetToResolved[sourceSet.name] = false
        }

        fun configureCompilation(compilation: KotlinCompilation<*>) {
            if (compilation.platformType !in setOf(KotlinPlatformType.common, KotlinPlatformType.jvm, KotlinPlatformType.js)) return

            val compileTask = compilation.compileTaskProvider.get() as KotlinCompileTool
            val taskName = compileTask.name.replace("compile", "generate")

            val primitivesTask = compilation.project.tasks.register(taskName, PrimitivesTask::class.java) { primitiveTask ->
                primitiveTask.usesService(primitivesCache)
                primitiveTask.primitivesCache.set(primitivesCache)

                primitiveTask.generationPath.set(primitivesExt.generationPath)
                primitiveTask.inputFiles.from(compileTask.sources)
                primitiveTask.libraries.from(compileTask.libraries)
                primitiveTask.compilation.set(compilation)
                primitiveTask.vectorize.set(primitivesExt.vectorize)
            }

            compileTask.dependsOn(primitivesTask)
            generalPrimitivesTask.get().dependsOn(primitivesTask)
        }

        if (kotlinExt is KotlinMultiplatformExtension) {
            kotlinExt.targets.all { target ->
                target.compilations.all { compilation ->
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
        const val primitivesCacheName = "primitivesCache"
        const val primitivesTaskName = "generateAllPrimitives"
        const val extensionName = "primitives"
    }
}
