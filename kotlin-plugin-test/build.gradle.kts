import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

group = rootProject.group
version = rootProject.version

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            repositories {
                gradlePluginPortal()
            }

            dependencies {
                api(kotlin("stdlib"))
                implementation(project(":primitives-annotations"))
            }
        }
    }
}

dependencies {
    kotlinCompilerPluginClasspath(project(":primitives-plugin:kotlin-plugin"))
}

val generatedDir = "$projectDir/src/commonMain/kotlin-gen"
val incrementalDir = "$buildDir/"

tasks.withType<KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-P",
            "plugin:io.kinference.primitives.kotlin-plugin:outputDir=$generatedDir",
            "-P",
            "plugin:io.kinference.primitives.kotlin-plugin:icOutputDir=$incrementalDir"
        )
    }
}

tasks["compileKotlinJs"].dependsOn("compileCommonMainKotlinMetadata")
tasks["compileKotlinJvm"].dependsOn("compileCommonMainKotlinMetadata")

//tasks["compileKotlin"].outputs.dir(generatedDir)

kotlin {
    sourceSets["commonMain"].apply {
        kotlin.srcDirs(generatedDir)
    }
}
