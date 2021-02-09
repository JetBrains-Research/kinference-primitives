import org.gradle.kotlin.dsl.provider.gradleKotlinDslOf

group = rootProject.group
version = rootProject.version

plugins {
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
                api(kotlin("stdlib"))

                implementation(project(":primitives-plugin:kotlin-plugin"))
                implementation(kotlin("compiler-embeddable"))
                implementation(kotlin("gradle-plugin-api", "1.4.30"))
            }
        }
    }
}
