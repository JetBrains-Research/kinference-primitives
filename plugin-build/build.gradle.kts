import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.util.Properties

Properties().apply {
    load(file("../gradle.properties").inputStream())

    forEach {
        ext.set(it.key.toString(), it.value)
    }
}

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMpp) apply false
    `maven-publish`
}

group = property("GROUP").toString()
version = property("VERSION").toString()

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

subprojects {
    apply {
        plugin("maven-publish")
    }

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

    publishing {
        repositories {
            maven {
                name = "SpacePackages"
                url = uri("https://packages.jetbrains.team/maven/p/ki/maven")

                credentials {
                    username = System.getenv("JB_SPACE_CLIENT_ID") ?: ""
                    password = System.getenv("JB_SPACE_CLIENT_SECRET") ?: ""
                }
            }
        }
    }
}

project.afterEvaluate {
    tasks.getByPath(":publishToMavenLocal").dependsOn(
        ":primitives-plugin:publishToMavenLocal",
        ":primitives-annotations:publishToMavenLocal"
    )

    tasks.getByPath(":publish").dependsOn(
        ":primitives-plugin:publish",
        ":primitives-annotations:publish"
    )
}
