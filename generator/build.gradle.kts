import tanvd.kosogor.proxy.publishJar
import tanvd.kosogor.proxy.publishPlugin

group = rootProject.group
version = rootProject.version

dependencies {
    api(kotlin("stdlib"))
    implementation(kotlin("compiler-embeddable"))

    implementation(kotlin("gradle-plugin-api"))

    implementation(project(":annotations"))

    implementation("com.squareup", "kotlinpoet", "1.6.0")
}

publishJar {
    publication {
        artifactId = "io.kinference.primitives"
    }
}

publishPlugin {
    id = "io.kinference"
    displayName = "primitives"
    implementationClass = "io.kinference.primitives.PrimitivesKotlinGradlePlugin"
    version = project.version.toString()

    info {
        description = "KInference Primitives Generator"
        website = "https://github.com/JetBrains-Research/kinference-primitives"
        tags.addAll(listOf("kotlin", "primitive", "performance", "generation"))
    }
}
