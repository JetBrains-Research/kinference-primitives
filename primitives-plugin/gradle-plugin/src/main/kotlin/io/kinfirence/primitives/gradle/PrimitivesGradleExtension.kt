package io.kinfirence.primitives.gradle

import java.io.File

@DslMarker
annotation class PrimitivesDSLTag

open class PrimitivesPluginExtension {
    var generationPath: File? = null
    var incrementalCachePath: File? = null
}

var primitives = PrimitivesPluginExtension()

@PrimitivesDSLTag
fun primitives(configure: PrimitivesPluginExtension.() -> Unit) {
    primitives.configure()
}
