package io.kinference.primitives.ic

import io.kinference.primitives.utils.JSON
import io.kinference.primitives.utils.sha256
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class ICCache(incrementalDir: File) {
    @Serializable
    data class Manifest(val inputsToOutputs: Map<FileData, Set<FileData>> = emptyMap()) {
        @Serializable
        data class FileData(val file: String, val hash: String) {
            constructor(file: KtFile) : this(file.virtualFilePath, file.text.sha256())
            constructor(file: File) : this(file.canonicalPath, file.sha256())
        }
    }

    data class State(val upToDate: UpToDate, val notUpToDate: NotUpToDate) {
        data class UpToDate(val inputsToOutputs: Map<KtFile, Set<File>>)
        data class NotUpToDate(val inputs: Set<KtFile>, val outputs: Set<File>)
    }

    private val manifestFile = File(incrementalDir, "incremental-primitives-data.json").also { it.parentFile.mkdirs() }
    private var manifest: Manifest
        get() = manifestFile.takeIf { it.exists() }?.readText()?.let { JSON.parse(Manifest.serializer(), it) } ?: Manifest()
        set(value) {
            manifestFile.writeText(JSON.string(Manifest.serializer(), value))
        }

    private fun getUnknownFiles(manifest: Manifest, outputs: Collection<File>): Set<File> {
        val allExpectedFiles = manifest.inputsToOutputs.values.flatMap { output -> output.map { it.file } }.toSet()
        return outputs.filter { it.file() !in allExpectedFiles }.toSet()
    }

    fun getState(inputs: Collection<KtFile>, outputs: Collection<File>): State {
        val manifest = manifest

        val upToDate = HashMap<KtFile, Set<File>>()
        val toRegenerate = HashSet<KtFile>()
        val toRemove = HashSet<File>()

        val unknown = getUnknownFiles(manifest, outputs)
        toRemove.addAll(unknown)

        for (input in inputs) {

            val icInput = manifest.inputsToOutputs.keys.find { it.file == input.virtualFilePath }
            if (icInput == null) {
                toRegenerate.add(input)
                continue
            }

            val icOutputs = manifest.inputsToOutputs[icInput]!!
            val icOutputsPath = icOutputs.map { it.file }.toSet()

            if (icInput.hash != input.text.sha256()) {
                toRegenerate.add(input)
                toRemove.addAll(outputs.filter { it.canonicalPath in icOutputsPath })
                continue
            }

            if (icOutputs.any { icOutput -> outputs.find { it.canonicalPath == icOutput.file }?.sha256() != icOutput.hash }) {
                toRegenerate.add(input)
                toRemove.addAll(outputs.filter { it.canonicalPath in icOutputsPath })
                continue
            }

            upToDate[input] = icOutputsPath.map { path -> outputs.single { it.canonicalPath == path } }.toSet()
        }

        return State(State.UpToDate(upToDate), State.NotUpToDate(toRegenerate, toRemove))
    }

    fun updateManifest(upToDate: State.UpToDate, inputsToOutputs: Map<KtFile, Set<File>>) {
        val total = upToDate.inputsToOutputs + inputsToOutputs
        manifest = Manifest(total.map { (input, outputs) -> Manifest.FileData(input) to outputs.map { Manifest.FileData(it) }.toSet() }.toMap())
    }

    private fun File.file() = canonicalPath
    private fun KtFile.file() = virtualFilePath

    private fun File.hash() = sha256()
    private fun KtFile.hash() = text.sha256()
}
