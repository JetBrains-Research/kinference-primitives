package io.kinference.primitives.analyze

import io.kinference.primitives.utils.Utils
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.storage.LockBasedStorageManager

object Analyze {
    fun analyzeCommonSources(configuration: CompilerConfiguration): Pair<AnalysisResult, KotlinCoreEnvironment> {
        val kotlinEnv = KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.METADATA_CONFIG_FILES)

        val sources = kotlinEnv.getSourceFiles()

        val dependenciesContainer =
            Utils.createKlibMetadataDependencyContainer(configuration, LockBasedStorageManager.NO_LOCKS)

        return CommonResolverForModuleFactory.analyzeFiles(
            files = sources,
            moduleName = Name.special("<primitives>"),
            dependOnBuiltIns = true,
            languageVersionSettings = configuration.languageVersionSettings,
            targetPlatform = CommonPlatforms.defaultCommonPlatform,
            targetEnvironment = CompilerEnvironment,
            dependenciesContainer = dependenciesContainer
        ) { kotlinEnv.createPackagePartProvider(it.moduleContentScope) } to kotlinEnv
    }

    fun analyzeJvmSources(configuration: CompilerConfiguration): Pair<AnalysisResult, KotlinCoreEnvironment> {
        val kotlinEnv = KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val project = kotlinEnv.project

        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            project = project,
            files = kotlinEnv.getSourceFiles(),
            trace = NoScopeRecordCliBindingTrace(project),
            configuration = configuration,
            packagePartProvider = kotlinEnv::createPackagePartProvider
        ) to kotlinEnv
    }

    fun analyzeJsSources(configuration: CompilerConfiguration): Pair<AnalysisResult, KotlinCoreEnvironment> {
        val kotlinEnv = KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)

        val libsPaths = configuration[CLIConfigurationKeys.CONTENT_ROOTS]?.filterIsInstance<JvmClasspathRoot>()?.map { it.file.absolutePath } ?: emptyList()

        val mainModule = MainModule.SourceFiles(kotlinEnv.getSourceFiles())
        val sourceModule = ModulesStructure(kotlinEnv.project, mainModule, configuration, libsPaths, emptyList())

        return TopDownAnalyzerFacadeForJSIR.analyzeFiles(
            mainModule.files,
            sourceModule.project,
            sourceModule.compilerConfiguration,
            sourceModule.descriptors.values.toList(),
            sourceModule.friendDependencies.map { sourceModule.getModuleDescriptor(it) },
            CompilerEnvironment,
        ) to kotlinEnv
    }

    fun createCompilerConfig(isMpp: Boolean): CompilerConfiguration  {
        val compilerConfiguration = CompilerConfiguration().apply {
            val features = mutableMapOf<LanguageFeature, LanguageFeature.State>()

            if (isMpp) {
                features[LanguageFeature.MultiPlatformProjects] = LanguageFeature.State.ENABLED
            }

            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
            put(CommonConfigurationKeys.MODULE_NAME, JvmProtoBufUtil.DEFAULT_MODULE_NAME)

            languageVersionSettings = LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.LATEST_STABLE,
                apiVersion = ApiVersion.LATEST_STABLE,
                specificFeatures = features
            )
        }

        return compilerConfiguration
    }
}
