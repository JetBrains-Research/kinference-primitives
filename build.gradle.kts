import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

group = "io.kinference.primitives"
version = "0.1.7"

plugins {
    id("tanvd.kosogor") version "1.0.10" apply true
    kotlin("jvm") version "1.4.20" apply false
    id("io.gitlab.arturbosch.detekt") version ("1.15.0-RC1") apply true
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("tanvd.kosogor")
    }

    if (name != "kotlin-plugin-test") {
        apply {
            plugin("io.gitlab.arturbosch.detekt")
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
            freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xopt-in=kotlin.ExperimentalUnsignedTypes")
        }
    }

    afterEvaluate {
        System.setProperty("gradle.publish.key", System.getenv("gradle_publish_key") ?: "")
        System.setProperty("gradle.publish.secret", System.getenv("gradle_publish_secret") ?: "")
    }
}
