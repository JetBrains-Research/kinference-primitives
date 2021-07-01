package io.kinference.primitives.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addExternalModuleDependencyTo
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.*

open class PrimitivesGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        return with(target) {

            target.afterEvaluate {
                val generationPath = project.file(project.primitives.generationPath)
                val icManifestPath = project.primitives.incrementalCachePath?.let { project.file(it) } ?: project.buildDir

                dependencies {
                    kotlinCompilerPluginClasspath("io.kinference.primitives", "kotlin-plugin", "0.1.14")
                }

                tasks.withType(KotlinCompile::class.java) {
                    it.kotlinOptions {
                        freeCompilerArgs = freeCompilerArgs + listOf(
                            "-P",
                            "plugin:io.kinference.primitives.kotlin-plugin:outputDir=${generationPath.absolutePath}",
                            "-P",
                            "plugin:io.kinference.primitives.kotlin-plugin:icOutputDir=${icManifestPath.absolutePath}"
                        )

                    }
                }

                tasks.getByName("compileKotlinJs").dependsOn("compileKotlinJvm")

                (extensions.findByName("kotlin") as KotlinProjectExtension).apply {
                    sourceSets.getByName("commonMain").apply {
                        kotlin.srcDir(generationPath)
                    }
                }
            }

        }
    }

    private fun DependencyHandler.kotlinCompilerPluginClasspath(
        group: String,
        name: String,
        version: String,
    ): ExternalModuleDependency = addExternalModuleDependencyTo(
        this, "kotlinCompilerPluginClasspath", group, name, version, null, null, null, null
    )
}
