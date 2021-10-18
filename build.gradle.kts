import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

group = "io.kinference.primitives"
version = "0.1.15"

plugins {
    kotlin("jvm") version "1.5.31" apply false
    kotlin("multiplatform") version "1.5.31" apply false
    id("tanvd.kosogor") version "1.0.12" apply false
}

subprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    tasks.withType<KotlinJvmCompile> {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.5"
            apiVersion = "1.5"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xopt-in=kotlin.ExperimentalUnsignedTypes")
        }
    }
}
