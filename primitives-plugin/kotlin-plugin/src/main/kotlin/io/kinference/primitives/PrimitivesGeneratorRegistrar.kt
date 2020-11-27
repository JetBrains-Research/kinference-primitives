package io.kinference.primitives

import com.google.auto.service.AutoService
import io.kinference.primitives.handler.PrimitivesGeneratorAnalysisHandler
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

@AutoService(ComponentRegistrar::class)
class PrimitivesGeneratorRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val outputDir = configuration[Keys.OUTPUT_DIR] ?: File("src/main/kotlin-gen")

        AnalysisHandlerExtension.registerExtension(project, PrimitivesGeneratorAnalysisHandler(outputDir))
    }
}
