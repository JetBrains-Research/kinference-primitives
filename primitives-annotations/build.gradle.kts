group = rootProject.group
version = rootProject.version

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
