import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("jvm") version "1.4.30" apply true
    id("io.kinference.primitives") version "0.1.9"
}

group = "io.kinference.primitives"
version = "0.1.9"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))
    api("io.kinference.primitives", "primitives-annotations", "0.1.9")
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.4"
        apiVersion = "1.4"
    }
}
