package io.kinfirence.primitives.gradle

import io.kinference.primitives.PrimitivesGeneratorCLProcessor
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.*

class PrimitivesGradleSubPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("primitives", PrimitivesPluginExtension::class.java)
        super.apply(target)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        TODO()
    }


    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        TODO()
    }

    override fun getCompilerPluginId(): String = PrimitivesGeneratorCLProcessor.PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact("io.kinference", "primitives", "0.1.2")

}
