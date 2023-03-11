package io.kinference.primitives

import com.google.auto.service.AutoService
import io.kinference.primitives.handler.PrimitivesGeneratorAnalysisHandler
import io.kinference.primitives.utils.psi.collector
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
internal class PrimitivesGeneratorRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = false

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val outputDir = configuration[Keys.OUTPUT_DIR] ?: error("Output path not specified")
        val incrementalDir = configuration[Keys.INCREMENTAL_DIR] ?: error("Incremental path not specified")

        AnalysisHandlerExtension.registerExtension(
            PrimitivesGeneratorAnalysisHandler(configuration.collector, outputDir, incrementalDir)
        )
    }
}
