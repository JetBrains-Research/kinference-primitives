import tanvd.kosogor.proxy.publishJar

group = rootProject.group
version = rootProject.version

dependencies {
    api(kotlin("stdlib"))
}

publishJar {
    publication {
        artifactId = "primitives-annotations"
    }
    bintray {
        username = "tanvd"
        repository = "io.kinference"
        info {
            description = "KInference Primitives"
        }
    }
}