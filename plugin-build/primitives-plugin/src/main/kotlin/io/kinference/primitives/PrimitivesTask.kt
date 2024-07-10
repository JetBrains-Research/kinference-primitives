package io.kinference.primitives

import io.kinference.primitives.analyze.Analyze
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.generator.PrimitiveGenerator
import io.kinference.primitives.utils.psi.isAnnotatedWith
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

abstract class PrimitivesTask : DefaultTask() {
    @get:Internal
    abstract val generationPath: DirectoryProperty

    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:Internal
    abstract val compilation: Property<KotlinCompilation<*>>

    @get:Internal
    abstract val primitivesCache: Property<PrimitivesCache>

    init {
        group = "generate"
        description = "Generates primitives from sources"
    }

    @TaskAction
    fun generate() {
        val kotlinExt = project.extensions.findByType(KotlinProjectExtension::class.java) ?: return
        if (compilation.get().allKotlinSourceSets.all { primitivesCache.get().sourceSetToResolved[it.name]!! }) return

        val isMpp = kotlinExt is KotlinMultiplatformExtension

        fun findSourceSetName(file: File): String {
            return kotlinExt.sourceSets.find { file in it.kotlin }!!.name
        }

        fun findSourceSetName(path: String): String = findSourceSetName(File(path))

        val sourcesWithMppInfo = inputFiles
            .filterNot { it.absolutePath.startsWith(generationPath.get().asFile.absolutePath) }
            .map { source ->
                val isMpp = findSourceSetName(source) == "commonMain"

                FileWithMpp(source, isMpp)
            }

        val analyzeFun = when(compilation.get().platformType) {
            KotlinPlatformType.jvm    -> Analyze::analyzeJvmSources
            KotlinPlatformType.js     -> Analyze::analyzeJsSources
            KotlinPlatformType.common -> Analyze::analyzeCommonSources
            else -> error("Unsupported platform type ${compilation.get().platformType}")
        }

        val compilerConfig = Analyze.createCompilerConfig(isMpp)

        for (source in sourcesWithMppInfo) {
            compilerConfig.addKotlinSourceRoot(source.file.path, source.isMpp)
        }

        compilerConfig.addJvmClasspathRoots(libraries.files.filterNotNull())

        val (result, kotlinEnv) = analyzeFun(compilerConfig)
        val ktSources = kotlinEnv.getSourceFiles()

        val annotated = ktSources.filter { it.isAnnotatedWith<GeneratePrimitives>(result.bindingContext) }
        val notGeneratedYet = annotated.filterNot { it.virtualFilePath in primitivesCache.get().resolvedPaths }

        for (ktFile in notGeneratedYet) {
            val sourceSet = findSourceSetName(ktFile.virtualFilePath)
            val outputDir = generationPath.dir(sourceSet).get().asFile

            PrimitiveGenerator(ktFile, result.bindingContext, outputDir, MessageCollector.NONE).generate()

            primitivesCache.get().resolvedPaths.add(ktFile.virtualFilePath)
        }

        compilation.get().allKotlinSourceSets.forEach { primitivesCache.get().sourceSetToResolved[it.name] = true }
    }


    data class FileWithMpp(val file: File, val isMpp: Boolean)
}
