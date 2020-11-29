package io.kinference.primitives

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

@AutoService(CommandLineProcessor::class)
class PrimitivesGeneratorCLProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(OUTPUT_DIR_OPTION, INCREMENTAL_DIR_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OUTPUT_DIR_OPTION -> configuration.put(Keys.OUTPUT_DIR, File(value))
            INCREMENTAL_DIR_OPTION -> configuration.put(Keys.INCREMENTAL_DIR, File(value))
        }
    }

    companion object {
        const val PLUGIN_ID = "io.kinference.primitives.kotlin-plugin"

        val OUTPUT_DIR_OPTION =
            CliOption(
                optionName = "outputDir",
                valueDescription = "<path>",
                description = "Resulting generated files",
                required = true,
                allowMultipleOccurrences = false
            )

        val INCREMENTAL_DIR_OPTION =
            CliOption(
                optionName = "icOutputDir",
                valueDescription = "<path>",
                description = "Temporary data for ic compilation",
                required = false,
                allowMultipleOccurrences = false
            )
    }
}
