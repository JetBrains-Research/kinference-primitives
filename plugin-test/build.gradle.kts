plugins {
    alias(libs.plugins.kotlin.mpp)
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
