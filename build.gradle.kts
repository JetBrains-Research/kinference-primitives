group = "io.kinference.primitives"
version = "0.1.8"

plugins {
    kotlin("multiplatform") version "1.4.30" apply false
    `maven-publish` apply true
}

subprojects {
    if (name != "primitives-plugin") {
        apply {
            plugin("org.jetbrains.kotlin.multiplatform")
            plugin("maven-publish")
        }

        publishing {
            repositories {
                maven {
                    name = "SpacePackages"
                    url = uri("https://packages.jetbrains.team/maven/p/ki/maven")

                    credentials {
                        username = System.getenv("PUBLISHER_ID")
                        password = System.getenv("PUBLISHER_KEY")
                    }
                }
            }
        }
    }

    repositories {
        jcenter()
        gradlePluginPortal()
    }
}
