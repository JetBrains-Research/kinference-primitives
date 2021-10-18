import org.gradle.kotlin.dsl.provider.gradleKotlinDslOf
import tanvd.kosogor.proxy.publishJar
import tanvd.kosogor.proxy.publishPlugin

group = rootProject.group
version = rootProject.version

plugins {
    kotlin("jvm")
    id("tanvd.kosogor")
}

publishJar {

}

publishPlugin {
    id = "io.kinference.primitives"
    displayName = "KinferencePrimitives"
    implementationClass = "io.kinference.primitives.gradle.PrimitivesGradlePlugin"
    version = rootProject.version.toString()
    info {
        website = "https://github.com/JetBrains-Research/kinference-primitives"
        vcsUrl = "https://github.com/JetBrains-Research/kinference-primitives"
        description = "KInference primitives generator"
        tags.addAll(listOf())
    }
}

dependencies {
    api(files(gradleKotlinDslOf(project)))
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("compiler-embeddable"))
    implementation(project(":primitives-plugin:utils"))

    api(kotlin("stdlib"))

    implementation(project(":primitives-plugin:kotlin-plugin"))
}
