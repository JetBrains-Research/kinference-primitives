package io.kinference.primitives.ic

import io.kinference.primitives.utils.JSON
import io.kinference.primitives.utils.sha256
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

internal class ICCache(incrementalDir: File) {
    @Serializable
    internal data class Manifest(
        val commonInputsToOutputs: Map<FileData, Set<FileData>> = emptyMap(),
        val inputsToOutputsByModule: Map<String, Map<FileData, Set<FileData>>> = emptyMap(),
    ) {
        @Serializable
        internal data class FileData(val file: String, val hash: String) {
            constructor(file: KtFile) : this(file.virtualFile.canonicalPath!!, file.text.sha256())
            constructor(file: File) : this(file.canonicalPath, file.sha256())

            fun isUpToDate() = File(file).takeIf { it.exists() }?.sha256() == hash
        }
        fun commonIsUpToDate(): Boolean {
            for ((input, outputs) in commonInputsToOutputs) {
                if (!input.isUpToDate() || !outputs.any { it.isUpToDate() }) {
                    return false
                }
            }

            return true
        }

        fun isUpToDateModule(module: String): Boolean {
            val inputsToOutputs = inputsToOutputsByModule[module] ?: return true
            for ((input, outputs) in inputsToOutputs) {
                if (!input.isUpToDate() || !outputs.any { it.isUpToDate() }) {
                    return false
                }
            }

            return true
        }
    }

    internal data class State(val upToDate: UpToDate, val notUpToDate: NotUpToDate) {
        data class UpToDate(val inputsToOutputs: Map<Manifest.FileData, Set<Manifest.FileData>>)
        data class NotUpToDate(val inputs: Set<KtFile>, val outputs: Set<File>)
    }

    private val manifestFile = File(incrementalDir, "incremental-primitives-data.json").also { it.parentFile.mkdirs() }
    private var manifest: Manifest
        get() = manifestFile.takeIf { it.exists() }?.readText()?.let { JSON.parse(Manifest.serializer(), it) } ?: Manifest()
        set(value) {
            manifestFile.writeText(JSON.string(Manifest.serializer(), value))
        }

    fun getMoreFiles(inputs: Collection<KtFile>, module: String): List<File> {
        val manifest = manifest

        if (manifest.isUpToDateModule(module)) return emptyList()

        val filePaths = inputs.map { it.virtualFile.canonicalPath!! }.toSet()
        val toAskMore = manifest.inputsToOutputsByModule[module]?.keys?.filter { it.file !in filePaths }
        return toAskMore?.map { File(it.file) }?.filter { it.exists() } ?: emptyList()
    }

    fun getMoreCommonFiles(inputs: Collection<KtFile>): List<File> {
        val manifest = manifest

        if (manifest.commonIsUpToDate()) return emptyList()

        val filePaths = inputs.map { it.virtualFile.canonicalPath!! }.toSet()
        val toAskMore = manifest.commonInputsToOutputs.keys.filter { it.file !in filePaths }

        return toAskMore.map { File(it.file) }.filter { it.exists() }
    }

    fun getCommonState(inputs: Collection<KtFile>, outputs: Collection<File>): State {
        val manifest = manifest

        val upToDate = HashMap<Manifest.FileData, Set<Manifest.FileData>>()
        val toRegenerate = HashSet<KtFile>()
        val toRemove = HashSet<File>()

        val unknown = getUnknownFilesCommon(manifest, outputs)
        toRemove.addAll(unknown)

        for (input in inputs) {
            val icInput = manifest.commonInputsToOutputs.keys.find { it.file == input.virtualFilePath }
            if (icInput == null) {
                toRegenerate.add(input)
                continue
            }

            val icOutputs = manifest.commonInputsToOutputs[icInput]!!
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

        for ((icInput, icOutputs) in manifest.commonInputsToOutputs) {
            if (icInput in upToDate) continue
            if (icInput.isUpToDate() && icOutputs.all { it.isUpToDate() }) {
                upToDate[icInput] = icOutputs
            }
        }

        return State(State.UpToDate(upToDate), State.NotUpToDate(toRegenerate, toRemove))
    }

    fun getStateByModule(inputs: Collection<KtFile>, outputs: Collection<File>, module: String): State {
        val manifest = manifest

        val commonInputs = manifest.commonInputsToOutputs.keys.map { it.file }
        val commonOutputs = manifest.commonInputsToOutputs.values.flatMap { output -> output.map { it.file } }

        val actualInputs = inputs.filter { it.virtualFile.canonicalPath!! !in commonInputs }
        val actualOutputs = outputs.filter { it.canonicalPath !in commonOutputs }

        val moduleInputsToOutputs = manifest.inputsToOutputsByModule[module] ?:
            return State(State.UpToDate(emptyMap()), State.NotUpToDate(actualInputs.toSet(), actualOutputs.toSet()))

        val upToDate = HashMap<Manifest.FileData, Set<Manifest.FileData>>()
        val toRegenerate = HashSet<KtFile>()
        val toRemove = HashSet<File>()

        val unknown = getUnknownFilesByModule(manifest, actualOutputs, module)
        toRemove.addAll(unknown)

        for (input in actualInputs) {
            val icInput = moduleInputsToOutputs.keys.find { it.file == input.virtualFilePath }
            if (icInput == null) {
                toRegenerate.add(input)
                continue
            }

            val icOutputs = moduleInputsToOutputs[icInput]!!
            val icOutputsPath = icOutputs.map { it.file }.toSet()

            if (icInput.hash != input.text.sha256()) {
                toRegenerate.add(input)
                toRemove.addAll(actualOutputs.filter { it.canonicalPath in icOutputsPath })
                continue
            }

            if (icOutputs.any { icOutput -> actualOutputs.find { it.canonicalPath == icOutput.file }?.sha256() != icOutput.hash }) {
                toRegenerate.add(input)
                toRemove.addAll(actualOutputs.filter { it.canonicalPath in icOutputsPath })
                continue
            }

            upToDate[icInput] = icOutputs
        }

        for ((icInput, icOutputs) in moduleInputsToOutputs) {
            if (icInput in upToDate) continue
            if (icInput.isUpToDate() && icOutputs.all { it.isUpToDate() }) {
                upToDate[icInput] = icOutputs
            }
        }

        return State(State.UpToDate(upToDate), State.NotUpToDate(toRegenerate, toRemove))
    }

    private fun getUnknownFilesCommon(manifest: Manifest, outputs: Collection<File>): Set<File> {
        val allExpectedFiles = manifest.commonInputsToOutputs.values.flatMap { output -> output.map { it.file } }.toSet()
        return outputs.filter { it.canonicalPath !in allExpectedFiles }.toSet()
    }

    private fun getUnknownFilesByModule(manifest: Manifest, outputs: Collection<File>, module: String): Set<File> {
        val moduleInputsToOutputs = manifest.inputsToOutputsByModule[module] ?: return outputs.toSet()
        val allExpectedFiles = moduleInputsToOutputs.values.flatMap { output -> output.map { it.file } }.toSet()
        return outputs.filter { it.canonicalPath !in allExpectedFiles }.toSet()
    }

    fun updateManifestCommon(upToDate: State.UpToDate, inputsToOutputs: Map<KtFile, Set<File>>) {
        val manifestOld = manifest
        val total = upToDate.inputsToOutputs +
            inputsToOutputs.map { (input, outputs) -> Manifest.FileData(input) to outputs.map { Manifest.FileData(it) }.toSet() }
        manifest = Manifest(total, manifestOld.inputsToOutputsByModule)
    }

    fun updateManifest(module: String, upToDate: State.UpToDate, inputsToOutputs: Map<KtFile, Set<File>>) {
        val manifestOld = manifest
        val total = upToDate.inputsToOutputs +
            inputsToOutputs.map { (input, outputs) -> Manifest.FileData(input) to outputs.map { Manifest.FileData(it) }.toSet() }
        val newModuleData = manifest.inputsToOutputsByModule.toMutableMap().apply { this[module] = total }

        manifest = Manifest(manifestOld.commonInputsToOutputs, newModuleData)
    }
}
