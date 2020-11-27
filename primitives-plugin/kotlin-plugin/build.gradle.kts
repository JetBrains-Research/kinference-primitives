group = rootProject.group
version = rootProject.version

dependencies {
    api(kotlin("stdlib"))
    implementation(kotlin("compiler-embeddable"))
    implementation(project(":primitives-annotations"))
}