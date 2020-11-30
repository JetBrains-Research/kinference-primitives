package io.kinference.primitives.gradle

import io.kinference.primitives.PrimitivesGeneratorCLProcessor
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("unused")
class PrimitivesGradleSubPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.pluginManager.apply("idea")
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val generationPath = project.file(project.primitives.generationPath)
        val icManifestPath = project.primitives.incrementalCachePath?.let { project.file(it) } ?: project.buildDir

        kotlinCompilation.defaultSourceSet {
            kotlin.srcDirs(generationPath)
        }
        project.tasks.getByName("compileKotlin").outputs.dir(generationPath)


        project.extensions.findByType(IdeaModel::class.java)?.let { model ->
            model.apply {
                module.generatedSourceDirs = module.generatedSourceDirs + generationPath
            }
        }

        return project.provider {
            listOf(
                SubpluginOption(PrimitivesGeneratorCLProcessor.OUTPUT_DIR_OPTION.optionName, generationPath.canonicalPath),
                SubpluginOption(PrimitivesGeneratorCLProcessor.INCREMENTAL_DIR_OPTION.optionName, icManifestPath.canonicalPath)
            )
        }
    }


    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = kotlinCompilation.platformType == KotlinPlatformType.jvm

    override fun getCompilerPluginId(): String = PrimitivesGeneratorCLProcessor.PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact("io.kinference.primitives", "kotlin-plugin", VERSION)

    companion object {
        const val VERSION = "0.1.4"
    }
}
