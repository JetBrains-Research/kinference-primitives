@file:Suppress("UNUSED_PARAMETER", "unused", "MemberVisibilityCanBePrivate", "UndocumentedPublicProperty", "UndocumentedPublicFunction")

package io.kinference.primitives.types

/**
 * Representation of primitive array (like [IntArray] or [BooleanArray]) that
 * is used in code that should be specialized.
 *
 * So, basically [PrimitiveArray] should be used as a substitution of some specific
 * type of array in code where you'd like to generate primitive specializations.
 *
 * Consider it some kind of C++ template variable.
 */
class PrimitiveArray(val size: Int) {
    constructor(size: Int, init: (Int) -> Any) : this(size)

    init {
        error("This class should not be used in runtime")
    }

    val indices: IntRange = 0 until size

    operator fun get(index: Int): PrimitiveType = throw UnsupportedOperationException()
    operator fun set(index: Int, value: PrimitiveType): PrimitiveType = throw UnsupportedOperationException()

    fun min(): PrimitiveType = throw UnsupportedOperationException()
    fun max(): PrimitiveType = throw UnsupportedOperationException()
    fun sum(): PrimitiveType = throw UnsupportedOperationException()

    fun fill(element: PrimitiveType, fromIndex: Int = 0, toIndex: Int = size): Unit = throw UnsupportedOperationException()

    fun sliceArray(indices: IntRange): PrimitiveArray = throw UnsupportedOperationException()

    fun copyOf(): PrimitiveArray = throw UnsupportedOperationException()
    fun copyOfRange(fromIndex: Int, toIndex: Int): PrimitiveArray = throw UnsupportedOperationException()
    fun copyInto(destination: PrimitiveArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = size): PrimitiveArray = throw UnsupportedOperationException()
}
