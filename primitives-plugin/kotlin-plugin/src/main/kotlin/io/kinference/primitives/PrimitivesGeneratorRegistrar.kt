package io.kinference.primitives

import com.google.auto.service.AutoService
import io.kinference.primitives.handler.PrimitivesGeneratorAnalysisHandler
import io.kinference.primitives.utils.collector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

@AutoService(ComponentRegistrar::class)
class PrimitivesGeneratorRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val outputDir = configuration[Keys.OUTPUT_DIR] ?: error("Output path not specified")
        val incrementalDir = configuration[Keys.INCREMENTAL_DIR] ?: error("Incremental path not specified")

        AnalysisHandlerExtension.registerExtension(
            project,
            PrimitivesGeneratorAnalysisHandler(configuration.collector, outputDir, incrementalDir)
        )
    }
}
