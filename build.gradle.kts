import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

group = "io.kinference.primitives"
//also change version in PluginConstants.kt
version = "0.1.26"

plugins {
    kotlin("multiplatform") version "1.9.21" apply false
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
                }/**/
            }
        }
    }


    extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
        sourceSets.all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }

            languageSettings {
                apiVersion = "1.9"
                languageVersion = "1.9"
            }
        }

        tasks.withType<KotlinJvmCompile> {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
}
