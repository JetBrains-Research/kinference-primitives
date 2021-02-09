group = rootProject.group
version = rootProject.version

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            repositories {
                jcenter()
                gradlePluginPortal()
            }

            dependencies {
                api(kotlin("stdlib"))
            }
        }
    }
}
