import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

group = property("GROUP").toString()
version = property("VERSION").toString()

plugins {
    alias(libs.plugins.kotlinMpp) apply false
}

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

subprojects {
    tasks.withType(JavaCompile::class.java).all {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    tasks.withType(KotlinCompilationTask::class.java).all {
        compilerOptions {
            if (this is KotlinJvmCompilerOptions) {
                jvmTarget.set(JvmTarget.JVM_1_8)
            }

            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }
}

tasks.register("publish") {
    group = "publishing"

    dependsOn(gradle.includedBuild("plugin-build").task(":publish"))
}

tasks.register("publishToMavenLocal") {
    group = "publishing"

    dependsOn(
        gradle.includedBuild("plugin-build").task(":publishToMavenLocal")
    )
}

