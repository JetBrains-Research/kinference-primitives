import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompile
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

afterEvaluate {
    val commonTasks = this@afterEvaluate.tasks.withType<KotlinCommonCompile>().toSet()
    val otherCompileTasks = this@afterEvaluate.tasks.withType<KotlinCompile<*>>().toSet().minus(commonTasks)
    val jarTasks = this@afterEvaluate.tasks.withType<Jar>().toSet()

    val commonTasksArray = commonTasks.toTypedArray()

    for (otherTask in otherCompileTasks + jarTasks) {
        otherTask.dependsOn(*commonTasksArray)
    }

    this.tasks.withType<KotlinCommonCompile>().whenTaskAdded {
        for (otherTask in otherCompileTasks + jarTasks) {
            otherTask.dependsOn(this@whenTaskAdded)
        }
    }
}

kotlin {
    sourceSets["commonMain"].apply {
        kotlin.srcDirs(generatedDir)
    }
}
