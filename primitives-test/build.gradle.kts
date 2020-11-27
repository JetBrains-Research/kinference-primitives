import io.kinference.primitives.primitives

plugins {
    kotlin("jvm") version "1.4.20"
    id("io.kinference.primitives") version "0.1.2"
}

group = "io.kinference.primitives"
version = "0.1.2"

repositories {
    mavenLocal()
    jcenter()
}

primitives {
    generationPath = file("src/main/kotlin-gen")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("io.kinference.primitives","io.kinference.primitives.gradle.plugin","0.1.2")
    api("io.kinference.primitives","primitives-annotations","0.1.2")
}
