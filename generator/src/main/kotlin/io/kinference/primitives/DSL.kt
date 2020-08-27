package io.kinference.primitives

import java.io.File

@DslMarker
annotation class PrimitivesDSLTag

open class PrimitivesPluginExtension {
    var generationPath: File? = null
    var generateOnImport: Boolean = false
    var beforeTasks: MutableList<String> = mutableListOf("classes")
}

var primitives = PrimitivesPluginExtension()

@PrimitivesDSLTag
fun primitives(configure: PrimitivesPluginExtension.() -> Unit) {
    primitives.configure()
}