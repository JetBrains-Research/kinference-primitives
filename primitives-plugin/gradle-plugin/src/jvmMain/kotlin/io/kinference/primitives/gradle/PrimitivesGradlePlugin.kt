package io.kinference.primitives.gradle

import io.kinference.primitives.PrimitivesGeneratorCLProcessor.Companion.INCREMENTAL_DIR_OPTION
import io.kinference.primitives.PrimitivesGeneratorCLProcessor.Companion.OUTPUT_DIR_OPTION
import io.kinference.primitives.utils.PluginConstants.ANNOTATIONS_ARTIFACT_ID
import io.kinference.primitives.utils.PluginConstants.GROUP_ID
import io.kinference.primitives.utils.PluginConstants.KOTLIN_PLUGIN_ARTIFACT_ID
import io.kinference.primitives.utils.PluginConstants.PLUGIN_ID
import io.kinference.primitives.utils.PluginConstants.VERSION
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

class PrimitivesGradlePlugin : KotlinCompilerPluginSupportPlugin {
    private var commonTasks = mutableSetOf<KotlinCompile<KotlinCommonOptions>>()
    private val tasksSet = mutableSetOf<KotlinCompile<KotlinCommonOptions>>()

    override fun apply(target: Project) {
        target.extensions.create("primitives", PrimitivesPluginExtension::class.java)
        target.apply { it.plugin(IdeaPlugin::class.java) }
        target.repositories {
            maven(url = "https://packages.jetbrains.team/maven/p/ki/maven")
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        when (kotlinCompilation.platformType) {
            KotlinPlatformType.common -> {
                commonTasks.add(kotlinCompilation.compileKotlinTask)
                tasksSet.forEach { it.dependsOn(kotlinCompilation.compileKotlinTask.name) }
            }
            else -> {
                tasksSet.add(kotlinCompilation.compileKotlinTask)
                for (commonTask in commonTasks) {
                    kotlinCompilation.compileKotlinTask.dependsOn(commonTask.name)
                }
            }
        }

        val project = kotlinCompilation.target.project
        val extension = project.primitives

        val actualGenPath = extension.generationPath?.let { project.file("$it/${kotlinCompilation.defaultSourceSetName}") } ?:
                            File(project.buildDir, "generated/source/primitives/${kotlinCompilation.defaultSourceSetName}")

        val icManifestPath = extension.incrementalCachePath?.let { project.file(it) } ?: project.buildDir

        kotlinCompilation.defaultSourceSet.apply {
            kotlin.srcDir(actualGenPath)
        }

        kotlinCompilation.dependencies {
            implementation("$GROUP_ID:$ANNOTATIONS_ARTIFACT_ID:$VERSION")
        }

        return project.provider {
            listOf(
                SubpluginOption(OUTPUT_DIR_OPTION.optionName, actualGenPath.absolutePath),
                SubpluginOption(INCREMENTAL_DIR_OPTION.optionName, icManifestPath.absolutePath)
            )
        }
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        GROUP_ID,
        KOTLIN_PLUGIN_ARTIFACT_ID,
        VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.platformType == KotlinPlatformType.common ||
        kotlinCompilation.platformType == KotlinPlatformType.jvm    ||
        kotlinCompilation.platformType == KotlinPlatformType.js

}
