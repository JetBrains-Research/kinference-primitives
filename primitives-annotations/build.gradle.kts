import tanvd.kosogor.proxy.publishJar

group = rootProject.group
version = rootProject.version

dependencies {
    api(kotlin("stdlib"))
}

publishJar {
    bintray {
        username = "tanvd"
        repository = "io.kinference"
        info {
            description = "KInference Primitives Annotations"
            githubRepo = "JetBrains-Research/kinference-primitives"
            vcsUrl = "https://github.com/JetBrains-Research/kinference-primitives"
            labels.addAll(listOf("kotlin", "primitives", "generation"))
        }
    }
}
