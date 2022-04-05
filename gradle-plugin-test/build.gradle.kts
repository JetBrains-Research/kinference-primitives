import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("multiplatform") version "1.6.20" apply true
    id("io.kinference.primitives") version "0.1.17"
}

group = "io.kinference.primitives"
version = "0.1.17"

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://packages.jetbrains.team/maven/p/ki/maven")
}

kotlin {
    jvm()
    js(BOTH) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.kinference.primitives:primitives-annotations:0.1.17")
            }
        }
    }
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.6"
        apiVersion = "1.6"
    }
}
