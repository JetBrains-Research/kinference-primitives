import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

group = "io.kinference.primitives"
version = "0.1.11"

plugins {
    kotlin("multiplatform") version "1.4.30" apply false
    `maven-publish`
}

subprojects {
    if (name != "primitives-plugin") {
        apply {
            plugin("org.jetbrains.kotlin.multiplatform")
            plugin("maven-publish")
        }

        publishing {
            repositories {
                maven {
                    name = "SpacePackages"
                    url = uri("https://packages.jetbrains.team/maven/p/ki/maven")

                    credentials {
                        username = System.getenv("PUBLISHER_ID")
                        password = System.getenv("PUBLISHER_KEY")
                    }
                }
            }
        }
    }

    repositories {
        mavenCentral()
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
}
