group = rootProject.group
version = rootProject.version

plugins {
    kotlin("multiplatform")
    `maven-publish`
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

kotlin {
    jvm()
    js(BOTH) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib"))
            }
        }
    }
}
