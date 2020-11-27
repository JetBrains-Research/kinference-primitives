package io.kinference.primitives.handler

import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiManagerImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class ICCache(
    private val manifestDir: File?
) {
    private val manifestFile = manifestDir?.let { File(it, "compiled-manifest.txt") }
    private val entries: MutableMap<String, String?> = readManifest()

    init {
        manifestDir?.mkdirs()
    }

    private fun readManifest(): MutableMap<String, String?> {
        val output = HashMap<String, String?>()

        if (manifestFile != null && manifestFile.exists()) {
            manifestFile.bufferedReader().useLines {
                it.forEach { line ->
                    val (path, hash) = line.split(":")
                    output[path] = hash
                }
            }
        }

        return output
    }



    private fun String.sha256(): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(this.toByteArray())
            .fold("", { str, it -> str + "%02x".format(it) })
    }

    fun analyzeInput(files: Collection<KtFile>): Collection<KtFile> {
        val toGenerate = mutableListOf<KtFile>()
        val toDelete = entries.keys.minus(files.map { file -> file.virtualFilePath })
        toDelete.forEach { entries[it] = null }

        for (file in files) {
            val path = file.virtualFilePath

            val oldHash = entries[path]
            val newHash = File(path).readText().sha256()

            if (oldHash == null || !oldHash.contentEquals(newHash)){
                toGenerate.add(file)
                entries[path] = newHash
            }
        }

        if (toGenerate.isNotEmpty() || toDelete.isNotEmpty()) {
            recordManifest()
        }

        return toGenerate
    }

    private fun recordManifest() {
        if (manifestFile != null) {
            manifestFile.createNewFile()
            FileOutputStream(manifestFile, false).bufferedWriter().use {
                entries.forEach { path, hash ->
                    it.write("$path:$hash\n")
                }
                it.flush()
            }
        }
    }
}
