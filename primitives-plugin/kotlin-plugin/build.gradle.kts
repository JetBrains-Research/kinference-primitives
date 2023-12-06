group = rootProject.group
version = rootProject.version

plugins {
    kotlin("plugin.serialization") version "1.9.21" apply true
    kotlin("kapt") apply true
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":primitives-plugin:utils"))
                implementation(project(":primitives-annotations"))
                implementation(kotlin("compiler-embeddable"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("com.google.auto.service:auto-service-annotations:1.0-rc7")
                configurations["kapt"].dependencies.add(implementation("com.google.auto.service:auto-service:1.0-rc7"))
            }
        }
    }
}
