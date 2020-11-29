import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

group = "io.kinference.primitives"
version = "0.1.3"

plugins {
    id("tanvd.kosogor") version "1.0.10" apply true
    kotlin("jvm") version "1.4.20" apply false
    id("io.gitlab.arturbosch.detekt") version ("1.11.0") apply true
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("tanvd.kosogor")
        plugin("io.gitlab.arturbosch.detekt")
    }

    repositories {
        jcenter()
        gradlePluginPortal()
    }

    tasks.withType<KotlinJvmCompile> {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.4"
            apiVersion = "1.4"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
        }
    }

    detekt {
        parallel = true

        config = rootProject.files("detekt.yml")

        reports {
            xml {
                enabled = false
            }
            html {
                enabled = false
            }
        }
    }

    afterEvaluate {
        System.setProperty("gradle.publish.key", System.getenv("gradle_publish_key") ?: "")
        System.setProperty("gradle.publish.secret", System.getenv("gradle_publish_secret") ?: "")
    }
}
