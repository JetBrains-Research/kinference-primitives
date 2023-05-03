package io.kinference.primitives.handler

import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.generator.PrimitiveGenerator
import io.kinference.primitives.ic.ICCache
import io.kinference.primitives.utils.psi.isAnnotatedWith
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.DeclarationScopeProvider
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
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

internal class PrimitivesGeneratorAnalysisHandler(
    private val collector: MessageCollector,
    private val outputDir: File,
    incrementalDir: File
) : AnalysisHandlerExtension {
    private val moduleName = outputDir.name
    private val cache = ICCache(incrementalDir)

    private fun resolveFilesAnnotations(files: Collection<KtFile>, componentProvider: ComponentProvider) {
        val declarationScopeProvider = componentProvider.get<DeclarationScopeProvider>()
        val fileScopeProvider = componentProvider.get<FileScopeProvider>()
        val declarationResolver = componentProvider.get<DeclarationResolver>()

        val context = TopDownAnalysisContext(TopDownAnalysisMode.TopLevelDeclarations, DataFlowInfo.EMPTY, declarationScopeProvider)

        for (file in files) {
            context.addFile(file)
        }

        declarationResolver.resolveAnnotationsOnFiles(context, fileScopeProvider)
    }

    private fun analyzeFiles(files: Collection<KtFile>, componentProvider: ComponentProvider) {
        val analyzer = componentProvider.get<LazyTopDownAnalyzer>()
        analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
    }

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        val context = bindingTrace.bindingContext
        val isCommon = module.platform.isCommon()
        outputDir.mkdirs()

        resolveFilesAnnotations(files, componentProvider)

        val allInputs = files.filter { it.isAnnotatedWith<GeneratePrimitives>(context) }
        val allOutputs = Files.walk(outputDir.toPath()).filter(Files::isRegularFile).map { it.toFile() }.collect(Collectors.toList()).toMutableSet()

        val moreFiles = if (isCommon) {
            cache.getMoreCommonFiles(allInputs)
        } else {
            cache.getMoreFiles(allInputs, moduleName) + cache.getMoreCommonFiles(allInputs)
        }

        if (moreFiles.isNotEmpty()) return AnalysisResult.RetryWithAdditionalRoots(BindingContext.EMPTY, module, emptyList(), moreFiles, emptyList(), true)

        val (upToDate, notUpToDate) = if (isCommon) {
            cache.getCommonState(allInputs, allOutputs)
        } else {
            cache.getStateByModule(allInputs, allOutputs, moduleName)
        }

        collector.report(CompilerMessageSeverity.LOGGING, "Primitives generator consider not up to date: $notUpToDate")

        notUpToDate.outputs.forEach { it.delete() }

        val inputsToOutputs = HashMap<KtFile, Set<File>>()

        analyzeFiles(notUpToDate.inputs, componentProvider)

        for (input in notUpToDate.inputs) {
            val result = PrimitiveGenerator(input, context, outputDir, collector).generate()
            inputsToOutputs[input] = result
        }

        if (isCommon) {
            cache.updateManifestCommon(upToDate, inputsToOutputs)
        } else {
            cache.updateManifest(moduleName, upToDate, inputsToOutputs)
        }

        collector.report(CompilerMessageSeverity.LOGGING, "Primitives generator generated: $inputsToOutputs")

        if (collector is GroupingMessageCollector) collector.flush()

        return when {
            inputsToOutputs.isEmpty() -> null
            else -> {
                AnalysisResult.RetryWithAdditionalRoots(
                    bindingContext = BindingContext.EMPTY,
                    moduleDescriptor = module,
                    additionalKotlinRoots = inputsToOutputs.values.flatten(),
                    additionalJavaRoots = emptyList(),
                    addToEnvironment = true
                )
            }
        }
    }
}

