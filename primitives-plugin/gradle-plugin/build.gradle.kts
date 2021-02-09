import org.gradle.kotlin.dsl.provider.gradleKotlinDslOf

group = rootProject.group
version = rootProject.version

plugins {
    id("com.gradle.plugin-publish") version "0.12.0"
    `java-gradle-plugin`
    `maven-publish`
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            repositories {
                jcenter()
                gradlePluginPortal()
            }

            dependencies {
                api(files(gradleKotlinDslOf(project)))
                compileOnly(kotlin("gradle-plugin", "1.4.30"))

                api(kotlin("stdlib"))

                implementation(project(":primitives-plugin:kotlin-plugin"))
            }
        }
    }
}
