group = rootProject.group
version = rootProject.version

plugins {
    alias(libs.plugins.kotlin.mpp) apply true
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
}

