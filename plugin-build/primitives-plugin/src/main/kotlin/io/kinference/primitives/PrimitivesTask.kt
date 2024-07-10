package io.kinference.primitives

import io.kinference.primitives.analyze.Analyze
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.generator.PrimitiveGenerator
import io.kinference.primitives.utils.psi.isAnnotatedWith
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool


abstract class PrimitivesTask : DefaultTask() {
    @get:OutputDirectory
    abstract val generationPath: DirectoryProperty

    @TaskAction
    fun generate() {
        val kotlinExt = project.extensions.findByType(KotlinProjectExtension::class.java) ?: return
        val (targets, isMpp) = when (kotlinExt) {
            is KotlinMultiplatformExtension -> kotlinExt.targets.toList() to true
            is KotlinSingleTargetExtension<*> -> listOf(kotlinExt.target) to false
            else -> throw GradleException("Incorrect KGP extension type, type: ${kotlinExt::class}")
        }
        val compilations = targets
            .flatMap { target -> target.compilations.toList() }
            .filterNot { compilation ->
                //It's a meta-compilation that doesn't have libraries
                (compilation.target.platformType == KotlinPlatformType.common && compilation.name == "main")
            }

        val pathToSourceSet: Map<String, String> = HashMap<String, String>().apply {
            for (sourceSet in kotlinExt.sourceSets) {
                val sourceSetName = sourceSet.name

                for (file in sourceSet.kotlin.files) {
                    this[file.path] = sourceSetName
                }
            }
        }

        val resolvedSourceSets = kotlinExt.sourceSets.associate { it.name to false }.toMutableMap()

        for (compilation in compilations) {
            // Check if all sourceSets of compilation are resolved
            if (compilation.allKotlinSourceSets.all { resolvedSourceSets[it.name]!! }) continue

            val analyzeFun = when (compilation) {
                is KotlinJvmCompilation -> Analyze::analyzeJvmSources
                is KotlinJsIrCompilation -> Analyze::analyzeJsSources
                is KotlinCommonCompilation -> Analyze::analyzeCommonSources
                else -> throw GradleException("Unsupported compilation target: ${compilation.target}")
            }

            val task = compilation.compileTaskProvider.get() as KotlinCompileTool

            val libs = task.libraries.files.filterNotNull()
            val sources = task.sources.files.filterNot { it.path.startsWith(generationPath.get().asFile.path) }.filterNotNull()

            if (sources.isEmpty()) {
                compilation.allKotlinSourceSets.forEach { resolvedSourceSets[it.name] = true }
                continue
            }

            val compilerConfig = Analyze.createCompilerConfig(isMpp)

            for (source in sources) {
                val isCommon = pathToSourceSet[source.path] == "commonMain"
                compilerConfig.addKotlinSourceRoot(source.path, isCommon)
            }
            compilerConfig.addJvmClasspathRoots(libs)

            val (result, kotlinEnv) = analyzeFun(compilerConfig)
            val ktSources = kotlinEnv.getSourceFiles()

            val annotated = ktSources.filter { it.isAnnotatedWith<GeneratePrimitives>(result.bindingContext) }
            val notGeneratedYet = annotated.filterNot { resolvedSourceSets[pathToSourceSet[it.virtualFilePath]!!]!! }

            for (ktFile in notGeneratedYet) {
                val sourceSet = pathToSourceSet[ktFile.virtualFilePath]!!
                val outputDir = generationPath.dir(sourceSet).get().asFile

                PrimitiveGenerator(ktFile, result.bindingContext, outputDir, MessageCollector.NONE).generate()
            }

            compilation.allKotlinSourceSets.forEach { resolvedSourceSets[it.name] = true }
        }
    }
}
