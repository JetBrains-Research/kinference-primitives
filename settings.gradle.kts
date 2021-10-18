rootProject.name = "kinference-primitives"

include(":primitives-annotations")
include(":primitives-plugin:kotlin-plugin")
include(":primitives-plugin:gradle-plugin")
include(":primitives-plugin:utils")
include(":kotlin-plugin-test")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
