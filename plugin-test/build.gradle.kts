plugins {
    alias(libs.plugins.kotlinMpp)
    id("io.kinference.primitives")
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain {
            dependencies {
                api("io.kinference.primitives:primitives-annotations")
            }
        }
    }
}
