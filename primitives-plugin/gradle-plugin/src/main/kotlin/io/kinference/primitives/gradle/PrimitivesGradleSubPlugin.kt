package io.kinference.primitives.gradle

import io.kinference.primitives.PrimitivesGeneratorCLProcessor
import org.gradle.api.Project
import org.gradle.api.provider.*
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

class PrimitivesGradleSubPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.apply { it.plugin(IdeaPlugin::class.java) }
        target.dependencies.add("api","io.kinference.primitives:primitives-annotations:$VERSION")
        target.extensions.create("primitives", PrimitivesPluginExtension::class.java)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension = project.extensions.findByType(PrimitivesPluginExtension::class.java) ?: PrimitivesPluginExtension()

        val outputPath = extension.generationPath ?: "src/main/kotlin-gen"
        val outputPathAbsolute = project.file(outputPath).absolutePath

        val icManifestPath = extension.incrementalCachePath?.let { project.file(it).absolutePath } ?: project.buildDir.absolutePath

        kotlinCompilation.defaultSourceSet {
            kotlin.srcDirs(outputPathAbsolute)
        }

        project.extensions.findByType(IdeaModel::class.java)?.let { model ->
            model.apply {
                module.generatedSourceDirs = module.generatedSourceDirs + File(outputPathAbsolute)
            }
        }

        return project.provider {
            listOf(
                SubpluginOption(PrimitivesGeneratorCLProcessor.OUTPUT_DIR_OPTION.optionName, outputPathAbsolute),
                SubpluginOption(PrimitivesGeneratorCLProcessor.INCREMENTAL_DIR_OPTION.optionName, icManifestPath)
            )
        }
    }


    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = kotlinCompilation.platformType == KotlinPlatformType.jvm

    override fun getCompilerPluginId(): String = PrimitivesGeneratorCLProcessor.PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact("io.kinference.primitives", "kotlin-plugin", VERSION)


    companion object {
        const val VERSION = "0.1.3"
    }
}
