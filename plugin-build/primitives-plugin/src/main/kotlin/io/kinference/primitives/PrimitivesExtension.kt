package io.kinference.primitives

import com.sun.org.apache.xpath.internal.operations.Bool
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import javax.inject.Inject

open class PrimitivesExtension @Inject constructor(
    project: Project
) {
    private val objects = project.objects

    val generationPath: DirectoryProperty = objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("generated/primitives")
    )
}
