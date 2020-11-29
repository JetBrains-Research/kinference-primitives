package io.kinference.primitives.utils

import java.io.File
import java.security.MessageDigest


fun <T> crossProduct(vararg collections: Collection<T>): List<List<T>> {
    if (collections.isEmpty()) return emptyList()

    val entries = collections.filter(Collection<T>::isNotEmpty)
    return entries.drop(1).fold(entries.first().map(::listOf)) { acc, entry ->
        acc.flatMap { list -> entry.map(list::plus) }
    }
}

fun File.sha256() = readText().sha256()
fun String.sha256() = hash("SHA-256")


fun File.md5() = readText().md5()
fun String.md5() = hash("MD5")

private fun String.hash(algorithm: String): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(this.toByteArray())
        .fold("", { str, it -> str + "%02x".format(it) })
}
