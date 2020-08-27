package io.kinference.primitives.utils.analysis

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil

object ResolveUtil {
    fun analyze(files: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult {
        return analyze(files, environment, environment.configuration)
    }

    private fun analyze(files: Collection<KtFile>, environment: KotlinCoreEnvironment, configuration: CompilerConfiguration): AnalysisResult {
        return analyze(environment.project, files, configuration, { factory -> environment.createPackagePartProvider(factory) })
    }

    private fun analyze(project: Project, files: Collection<KtFile>, configuration: CompilerConfiguration,
                        factory: (GlobalSearchScope) -> PackagePartProvider, trace: BindingTrace = CliBindingTrace()): AnalysisResult {
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project, files, trace, configuration, factory
        )
    }
}

/** Forcefully resolves all contents inside KtElement or Descriptor */
fun <T> T.forced(): T = ForceResolveUtil.forceResolveAllContents(this)
