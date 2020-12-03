package io.kinference.primitives.types.number


@Suppress("unused", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
class PrimitiveNumberArray(val size: Int) {
    constructor(size: Int, init: (Int) -> Any) : this(size)

    init {
        error("This class should not be used in runtime")
    }

    val indices: IntRange = 0 until size

    operator fun get(index: Int): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun set(index: Int, value: PrimitiveNumberType): PrimitiveNumberType = throw UnsupportedOperationException()

    fun min(): PrimitiveNumberType = throw UnsupportedOperationException()
    fun max(): PrimitiveNumberType = throw UnsupportedOperationException()
    fun sum(): PrimitiveNumberType = throw UnsupportedOperationException()

    fun fill(element: PrimitiveNumberType, fromIndex: Int = 0, toIndex: Int = size): Unit = throw UnsupportedOperationException()

    fun sliceArray(indices: IntRange): PrimitiveNumberArray = throw UnsupportedOperationException()

    fun copyOf(): PrimitiveNumberArray = throw UnsupportedOperationException()
    fun copyOfRange(fromIndex: Int, toIndex: Int): PrimitiveNumberArray = throw UnsupportedOperationException()
    fun copyInto(destination: PrimitiveNumberArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = size): PrimitiveNumberArray = throw UnsupportedOperationException()
}
