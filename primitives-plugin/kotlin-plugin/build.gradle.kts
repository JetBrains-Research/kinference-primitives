import tanvd.kosogor.proxy.publishJar

group = rootProject.group
version = rootProject.version

plugins {
    kotlin("jvm")
    id("tanvd.kosogor")
    kotlin("plugin.serialization") version "1.5.31" apply true
    kotlin("kapt") apply true
}

dependencies {
    implementation(project(":primitives-plugin:utils"))
    implementation(project(":primitives-annotations"))
    implementation(kotlin("compiler-embeddable"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("com.google.auto.service:auto-service-annotations:1.0-rc7")
    kapt("com.google.auto.service:auto-service:1.0-rc7")
}

publishJar {
}
