import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy

plugins {
    kotlin("multiplatform") version "1.8.10" apply true
    id("io.kinference.primitives") version "0.1.21"
}

group = "io.kinference.primitives"
version = "0.1.21"

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://packages.jetbrains.team/maven/p/ki/maven")
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.kinference.primitives:primitives-annotations:0.1.21")
            }
        }
    }
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "1.7"
        apiVersion = "1.7"
    }
}

// Required for kotlin compiler debugging
tasks.withType<CompileUsingKotlinDaemon>().configureEach {
    compilerExecutionStrategy.set(KotlinCompilerExecutionStrategy.IN_PROCESS)
}
