rootProject.name = "kinference-primitives"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

includeBuild("./plugin-build")
include(":plugin-test")
