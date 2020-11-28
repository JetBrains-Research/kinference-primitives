package io.kinference.primitives.ic

import io.kinference.primitives.utils.JSON
import io.kinference.primitives.utils.sha256
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class ICCache(incrementalDir: File) {
    @Serializable
    data class Manifest(val inputs: List<FileData> = emptyList(), val outputs: List<FileData> = emptyList()) {
        @Serializable
        data class FileData(val file: String, val hash: String)
    }

    private val manifestFile = File(incrementalDir, "incremental-primitives-data.json").also { it.parentFile.mkdirs() }
    private var manifest: Manifest
        get() = manifestFile.takeIf { it.exists() }?.readText()?.let { JSON.parse(Manifest.serializer(), it) } ?: Manifest()
        set(value) {
            manifestFile.writeText(JSON.string(Manifest.serializer(), value))
        }

    fun shouldRestartByInputs(files: Collection<KtFile>): Boolean {
        val inputs = manifest.inputs.map { it.file to it.hash }.toMap()
        for (file in files) {
            val expected = inputs[file.virtualFilePath] ?: return true
            val current = file.text.sha256()
            if (expected != current) return true
        }

        return false
    }

    fun shouldRestartByOutputs(files: Collection<File>): Boolean {
        val outputs = manifest.outputs.map { it.file to it.hash }.toMap()
        for (file in files) {
            val expected = outputs[file.canonicalPath] ?: return true
            val current = file.readText().sha256()
            if (expected != current) return true
        }

        return false
    }

    fun updateManifest(inputs: Collection<KtFile>, outputs: Collection<File>) {
        manifest = Manifest(
            inputs = inputs.map { Manifest.FileData(it.virtualFilePath, it.text.sha256()) },
            outputs = outputs.map { Manifest.FileData(it.canonicalPath, it.readText().sha256()) }
        )
    }
}
