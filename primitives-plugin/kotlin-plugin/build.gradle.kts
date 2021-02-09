group = rootProject.group
version = rootProject.version



plugins {
    kotlin("plugin.serialization") version "1.4.30" apply true
    kotlin("kapt") apply true
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":primitives-annotations"))
            }
        }
        val jvmMain by getting {
            repositories {
                jcenter()
                gradlePluginPortal()
            }

            dependencies {
                compileOnly(kotlin("compiler-embeddable"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
                implementation("com.google.auto.service:auto-service-annotations:1.0-rc7")
                configurations["kapt"].dependencies.add(implementation("com.google.auto.service:auto-service:1.0-rc7"))
            }
        }
    }
}

