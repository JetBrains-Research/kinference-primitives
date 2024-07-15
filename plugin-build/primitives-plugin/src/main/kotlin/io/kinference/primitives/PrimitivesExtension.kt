package io.kinference.primitives

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import javax.inject.Inject

open class PrimitivesExtension @Inject constructor(
    project: Project
) {
    private val objects = project.objects

    val generationPath: DirectoryProperty = objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("generated/primitives")
    )
}
