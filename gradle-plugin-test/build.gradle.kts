plugins {
    kotlin("jvm") version "1.4.20"
    id("io.kinference.primitives") version "0.1.3"
}

group = "io.kinference.primitives"
version = "0.1.3"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.4"
        apiVersion = "1.4"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
    }
}

primitives {
    generationPath = "src/main/kotlin-gen"
}
