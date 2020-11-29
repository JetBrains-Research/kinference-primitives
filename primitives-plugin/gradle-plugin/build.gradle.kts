import tanvd.kosogor.proxy.publishJar
import tanvd.kosogor.proxy.publishPlugin

group = rootProject.group
version = rootProject.version

dependencies {
    api(kotlin("stdlib"))
    implementation(project(":primitives-plugin:kotlin-plugin"))
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("gradle-plugin-api", "1.4.20"))
}

publishJar {
    publication {
        artifactId = "io.kinference.primitives.gradle.plugin"
    }
}

publishPlugin {
    id = "io.kinference.primitives"
    displayName = "primitives"
    implementationClass = "io.kinference.primitives.gradle.PrimitivesGradleSubPlugin"
    version = project.version.toString()

    info {
        description = "KInference Primitives Generator"
        website = "https://github.com/JetBrains-Research/kinference-primitives"
        vcsUrl = "https://github.com/JetBrains-Research/kinference-primitives"
        tags.addAll(listOf("kotlin", "primitive", "performance", "generation"))
    }
}
