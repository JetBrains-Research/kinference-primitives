group = "io.kinference.primitives"
version = "0.1.8"

plugins {
    kotlin("multiplatform") version "1.4.30" apply false
    `maven-publish` apply true
}

subprojects {
    if (name != "primitives-plugin") {
        apply {
            plugin("org.jetbrains.kotlin.multiplatform")
            plugin("maven-publish")
        }

        publishing {
            repositories {
                mavenLocal()
            }
        }
    }

    repositories {
        jcenter()
        gradlePluginPortal()
    }
}
