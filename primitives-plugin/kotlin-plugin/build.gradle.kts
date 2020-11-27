group = rootProject.group
version = rootProject.version

plugins {
    kotlin("kapt") apply true
}

dependencies {
    implementation(project(":primitives-annotations"))

    compileOnly(kotlin("compiler-embeddable"))

    implementation("com.google.auto.service", "auto-service-annotations", "1.0-rc7")
    kapt("com.google.auto.service", "auto-service", "1.0-rc7")
}