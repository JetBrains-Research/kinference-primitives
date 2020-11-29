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
            constructor(file: KtFile) : this(file.virtualFile.canonicalPath!!, file.text.sha256())
            constructor(file: File) : this(file.canonicalPath, file.sha256())

            fun isUpToDate() = File(file).takeIf { it.exists() }?.sha256() == hash
        }

        fun isUpToDate(): Boolean {
            for ((input, outputs) in inputsToOutputs) {
                if (!input.isUpToDate() || !outputs.any { it.isUpToDate() }) {
                    return false
                }
            }

            return true
        }
    }

    data class State(val upToDate: UpToDate, val notUpToDate: NotUpToDate) {
        data class UpToDate(val inputsToOutputs: Map<Manifest.FileData, Set<Manifest.FileData>>)
        data class NotUpToDate(val inputs: Set<KtFile>, val outputs: Set<File>)
    }

    private val manifestFile = File(incrementalDir, "incremental-primitives-data.json").also { it.parentFile.mkdirs() }
    private var manifest: Manifest
        get() = manifestFile.takeIf { it.exists() }?.readText()?.let { JSON.parse(Manifest.serializer(), it) } ?: Manifest()
        set(value) {
            manifestFile.writeText(JSON.string(Manifest.serializer(), value))
        }

    fun getState(inputs: Collection<KtFile>, outputs: Collection<File>): State {
        val manifest = manifest

        val upToDate = HashMap<Manifest.FileData, Set<Manifest.FileData>>()
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

            upToDate[icInput] = icOutputs
        }

        for ((icInput, icOutputs) in manifest.inputsToOutputs) {
            if (icInput in upToDate) continue
            if (icInput.isUpToDate() && icOutputs.all { it.isUpToDate() }) {
                upToDate[icInput] = icOutputs
            }
        }

        return State(State.UpToDate(upToDate), State.NotUpToDate(toRegenerate, toRemove))
    }

    private fun getUnknownFiles(manifest: Manifest, outputs: Collection<File>): Set<File> {
        val allExpectedFiles = manifest.inputsToOutputs.values.flatMap { output -> output.map { it.file } }.toSet()
        return outputs.filter { it.canonicalPath !in allExpectedFiles }.toSet()
    }

    fun updateManifest(upToDate: State.UpToDate, inputsToOutputs: Map<KtFile, Set<File>>) {
        val total = upToDate.inputsToOutputs +
            inputsToOutputs.map { (input, outputs) -> Manifest.FileData(input) to outputs.map { Manifest.FileData(it) }.toSet() }
        manifest = Manifest(total.toMap())
    }
}
