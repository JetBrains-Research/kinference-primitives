package io.kinference.primitives

import com.google.auto.service.AutoService
import io.kinference.primitives.utils.PluginConstants.PLUGIN_ID
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

/**
 * Main interface of Primitives generator plugin that
 * interacts with Gradle sub-plugin
 */
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
        /** Folder to which files should be generated */
        val OUTPUT_DIR_OPTION =
            CliOption(
                optionName = "outputDir",
                valueDescription = "<path>",
                description = "Resulting generated files",
                required = true,
                allowMultipleOccurrences = false
            )

        /** Folder in which incremental cache of primitives plugin can create files */
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
