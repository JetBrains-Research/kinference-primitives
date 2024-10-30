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
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.mpp) apply false
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
        publications {
            all {
                if (this !is MavenPublication) return@all

                pom {
                    name = "KInference Primitives"
                    description =
                        "KInference Primitives is a library that makes possible generation of primitive versions for generic types.\n" +
                        "\n" +
                        "It supports the Kotlin Multiplatform and is capable of generating common code that would be possible to reuse between JS and JVM " +
                        "targets."

                    licenses {
                        license {
                            name = "Apache License, Version 2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0"
                        }
                    }

                    scm {
                        url = "https://github.com/JetBrains-Research/kinference-primitives"
                    }
                }
            }
        }

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
