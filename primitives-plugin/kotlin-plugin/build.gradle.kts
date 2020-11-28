import tanvd.kosogor.proxy.publishJar

group = rootProject.group
version = rootProject.version

plugins {
    kotlin("kapt") apply true
    kotlin("plugin.serialization") version "1.4.20" apply true
}

dependencies {
    implementation(project(":primitives-annotations"))

    compileOnly(kotlin("compiler-embeddable"))

    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.0.1")

    implementation("com.google.auto.service", "auto-service-annotations", "1.0-rc7")
    kapt("com.google.auto.service", "auto-service", "1.0-rc7")
}

publishJar {
    bintray {
        username = "tanvd"
        repository = "io.kinference"
        info {
            description = "KInference Primitives Kotlin Plugin Compiler"
            githubRepo = "JetBrains-Research/kinference-primitives"
            vcsUrl = "https://github.com/JetBrains-Research/kinference-primitives"
            labels.addAll(listOf("kotlin", "primitives", "generation"))
        }
    }
}
