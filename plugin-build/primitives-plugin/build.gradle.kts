group = rootProject.group
version = rootProject.version

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish) apply true
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("compiler-embeddable"))
    implementation(project(":primitives-annotations"))
}


gradlePlugin {
    plugins {
        create("Primitives") {
            id = group.toString()
            implementationClass = "io.kinference.primitives.PrimitivesGradlePlugin"
            version = version.toString()
        }
    }
}
