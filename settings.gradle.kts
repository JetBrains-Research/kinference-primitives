rootProject.name = "kinference-primitives"

include(":primitives-annotations")
include(":primitives-generator")
include("primitives-plugin:kotlin-plugin")
include("primitives-test-2")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
