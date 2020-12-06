package io.kinference.primitives.handler

import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.generator.PrimitiveGenerator
import io.kinference.primitives.ic.ICCache
import io.kinference.primitives.utils.psi.isAnnotatedWith
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File
import java.nio.file.Files
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.flatten
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.set
import kotlin.collections.toMutableSet
import kotlin.streams.toList

internal class PrimitivesGeneratorAnalysisHandler(
    private val collector: MessageCollector,
    private val outputDir: File,
    incrementalDir: File
) : AnalysisHandlerExtension {
    private val cache = ICCache(incrementalDir)

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        val context = bindingTrace.bindingContext

        outputDir.mkdirs()

        val allInputs = files.filter { it.isAnnotatedWith<GeneratePrimitives>(context) }
        val allOutputs = Files.walk(outputDir.toPath()).filter(Files::isRegularFile).map { it.toFile() }.toList().toMutableSet()

        val moreFiles = cache.getMoreFiles(allInputs)
        if (moreFiles.isNotEmpty()) return AnalysisResult.RetryWithAdditionalRoots(context, module, emptyList(), moreFiles, true )

        val (upToDate, notUpToDate) = cache.getState(allInputs, allOutputs)

        collector.report(CompilerMessageSeverity.LOGGING, "Primitives generator consider not up to date: $notUpToDate")

        notUpToDate.outputs.forEach { it.delete() }

        val inputsToOutputs = HashMap<KtFile, Set<File>>()
        for (input in notUpToDate.inputs) {
            val result = PrimitiveGenerator(input, context, outputDir, collector).generate()
            inputsToOutputs[input] = result
        }

        cache.updateManifest(upToDate, inputsToOutputs)

        collector.report(CompilerMessageSeverity.LOGGING, "Primitives generator generated: $inputsToOutputs")

        if (collector is GroupingMessageCollector) collector.flush()

        return when {
            inputsToOutputs.isEmpty() -> null
            else -> {
                AnalysisResult.RetryWithAdditionalRoots(
                    bindingContext = context,
                    moduleDescriptor = module,
                    additionalKotlinRoots = inputsToOutputs.values.flatten(),
                    additionalJavaRoots = emptyList(),
                    addToEnvironment = true
                )
            }
        }
    }
}

