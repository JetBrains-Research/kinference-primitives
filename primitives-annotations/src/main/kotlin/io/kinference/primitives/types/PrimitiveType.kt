package io.kinference.primitives.types

@Suppress("unused", "UNUSED_PARAMETER")
class PrimitiveType {
    companion object {
        val MIN_VALUE: PrimitiveType = PrimitiveType()
        val MAX_VALUE: PrimitiveType = PrimitiveType()

        const val SIZE_BYTES: Int = 0
        const val SIZE_BITS: Int = 0
    }

    init {
        error("Don't use this class in runtime")
    }

    operator fun plus(other: PrimitiveType): PrimitiveType = throw UnsupportedOperationException()
    operator fun minus(other: PrimitiveType): PrimitiveType = throw UnsupportedOperationException()
    operator fun times(other: PrimitiveType): PrimitiveType = throw UnsupportedOperationException()
    operator fun div(other: PrimitiveType): PrimitiveType = throw UnsupportedOperationException()
    operator fun rem(other: PrimitiveType): PrimitiveType = throw UnsupportedOperationException()

    operator fun inc(): PrimitiveType = throw UnsupportedOperationException()
    operator fun dec(): PrimitiveType = throw UnsupportedOperationException()
    operator fun unaryPlus(): PrimitiveType = throw UnsupportedOperationException()
    operator fun unaryMinus(): PrimitiveType = throw UnsupportedOperationException()

    operator fun compareTo(other: PrimitiveType): Int = throw UnsupportedOperationException()

    fun toPrimitive(): PrimitiveType = throw UnsupportedOperationException()
}

@Suppress("unused")
fun Number.toPrimitive(): PrimitiveType = throw UnsupportedOperationException()
