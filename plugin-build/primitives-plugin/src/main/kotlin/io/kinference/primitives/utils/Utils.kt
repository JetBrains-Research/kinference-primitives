package io.kinference.primitives.utils

import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.storage.StorageManager
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

internal object Utils {
    private inline fun <reified T : Any> createPrivateClass(className: String, vararg args: Any?): T {
        val kClass = Class.forName(className).kotlin
        val constructor = kClass.primaryConstructor ?: error("Constructor for $className not found")
        constructor.isAccessible = true
        val result = constructor.call(*args) as T
        constructor.isAccessible = false
        return result
    }

    fun createKlibMetadataDependencyContainer(configuration: CompilerConfiguration, storageManager: StorageManager): CommonDependenciesContainer {
        return createPrivateClass(
            "org.jetbrains.kotlin.cli.metadata.KlibMetadataDependencyContainer",
            configuration, storageManager
        )
    }
}


internal fun <T> crossProduct(vararg collections: Collection<T>): List<List<T>> {
    if (collections.all { it.isEmpty() }) return emptyList()

    val entries = collections.filter { it.isNotEmpty() }
    return entries.drop(1).fold(entries.first().map(::listOf)) { acc, entry ->
        acc.flatMap { list -> entry.map(list::plus) }
    }
}
