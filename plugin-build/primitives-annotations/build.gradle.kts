group = rootProject.group
version = rootProject.version

plugins {
    alias(libs.plugins.kotlinMpp) apply true
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
}

