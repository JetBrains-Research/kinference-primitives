import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

group = "io.kinference.primitives"
version = "0.1.15"

plugins {
    kotlin("multiplatform") version "1.5.31" apply false
    `maven-publish` apply true
}

subprojects {
    if (this.subprojects.isNotEmpty()) return@subprojects
    if (this.name == "kotlin-plugin-test") {
        apply {
            plugin("org.jetbrains.kotlin.multiplatform")

        }
        return@subprojects
    }


    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

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
                    username = System.getenv("JB_SPACE_CLIENT_ID") ?: ""
                    password = System.getenv("JB_SPACE_CLIENT_SECRET") ?: ""
                }
            }
        }
    }


    extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
        sourceSets.all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }

            languageSettings {
                apiVersion = "1.5"
                languageVersion = "1.5"
            }
        }

        tasks.withType<KotlinJvmCompile> {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
}
