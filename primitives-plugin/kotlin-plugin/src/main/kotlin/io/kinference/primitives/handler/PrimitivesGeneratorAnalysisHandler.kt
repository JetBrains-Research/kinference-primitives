package io.kinference.primitives.handler

import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.generator.PrimitiveGenerator
import io.kinference.primitives.generator.ReplacementProcessor
import io.kinference.primitives.ic.ICCache
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf.fqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.io.File
import java.nio.file.Files
import kotlin.streams.toList

class PrimitivesGeneratorAnalysisHandler(
    private val collector: MessageCollector,
    private val outputDir: File,
    incrementalDir: File
) : AnalysisHandlerExtension {
    companion object {
    }

    private val cache = ICCache(incrementalDir)

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        val resolveSession = componentProvider.get<ResolveSession>()
        val context = bindingTrace.bindingContext

        outputDir.mkdirs()

        val allInputs = files.filter { resolveSession.getFileAnnotations(it).hasAnnotation(GeneratePrimitives::class.fqName) }
        val allOutputs = Files.walk(outputDir.toPath()).filter(Files::isRegularFile).map { it.toFile() }.toList().toMutableSet()

        val (upToDate, notUpToDate) = cache.getState(allInputs, allOutputs)

        collector.report(CompilerMessageSeverity.LOGGING, "Primitives generator consider not up to date: $notUpToDate")

        notUpToDate.outputs.forEach { it.delete() }

        //recreate
        //TODO-tanvd should be allInputs here
        componentProvider.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, notUpToDate.inputs)

        val replacementContext = ReplacementProcessor.prepareGlobalContext(context, notUpToDate.inputs.toSet())

        val inputsToOutputs = HashMap<KtFile, Set<File>>()
        for (input in notUpToDate.inputs) {
            val result = PrimitiveGenerator(input, context, outputDir, collector, replacementContext).generate()
            inputsToOutputs[input] = result
        }

        cache.updateManifest(upToDate, inputsToOutputs)

        collector.report(CompilerMessageSeverity.LOGGING, "Primitives generator generated: $inputsToOutputs")

        return when {
            inputsToOutputs.isEmpty() -> null
            else -> {
                AnalysisResult.RetryWithAdditionalRoots(
                    bindingContext = bindingTrace.bindingContext,
                    moduleDescriptor = module,
                    additionalKotlinRoots = inputsToOutputs.values.flatten(),
                    additionalJavaRoots = emptyList(),
                    addToEnvironment = true
                )
            }
        }
    }
}

