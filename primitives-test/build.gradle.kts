import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

group = rootProject.group
version = rootProject.version

plugins {
    idea
}

dependencies {
    implementation(kotlin("stdlib"))
    kotlinCompilerPluginClasspath(project(":primitives-plugin:kotlin-plugin"))
    implementation(project(":primitives-annotations"))
}

val generatedDir = "$projectDir/src/main/kotlin-gen"
val incrementalDir = "$buildDir/"

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-P",
            "plugin:io.kinference.primitives:outputDir=$generatedDir",
            "-P",
            "plugin:io.kinference.primitives:icOutputDir=$incrementalDir"
        )
    }
}

idea {
    module.generatedSourceDirs.plusAssign(files(generatedDir))
}

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDirs(generatedDir)
    }
}
