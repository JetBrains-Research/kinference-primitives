package io.kinference.primitives

import io.kinference.primitives.PrimitivesGeneratorCLProcessor.Companion.PLUGIN_ID
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

object Keys {
    val OUTPUT_DIR = CompilerConfigurationKey.create<File>("$PLUGIN_ID.outputDir")
    val INCREMENTAL_DIR = CompilerConfigurationKey.create<File>("$PLUGIN_ID.icOutputDir")
}
