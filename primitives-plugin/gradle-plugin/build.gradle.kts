import org.gradle.kotlin.dsl.provider.gradleKotlinDslOf

group = rootProject.group
version = rootProject.version


plugins {
    id("com.gradle.plugin-publish") version "0.12.0"
    `maven-publish`
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val jvmMain by getting {
            repositories {
                mavenCentral()
                gradlePluginPortal()
            }

            dependencies {
                api(files(gradleKotlinDslOf(project)))
                implementation(kotlin("gradle-plugin"))
                implementation(kotlin("compiler-embeddable"))
                implementation(project(":primitives-plugin:utils"))

                api(kotlin("stdlib"))

                implementation(project(":primitives-plugin:kotlin-plugin"))
            }
        }
    }
}
