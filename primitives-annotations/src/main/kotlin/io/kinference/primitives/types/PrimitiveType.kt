@file:Suppress("unused", "UNUSED_PARAMETER")

package io.kinference.primitives.types

/**
 * Primitive type that can represent only number types
 */
abstract class PrimitiveType : Number() {
    companion object {
        val MIN_VALUE: PrimitiveType
            get() = throw UnsupportedOperationException()
        val MAX_VALUE: PrimitiveType
            get() = throw UnsupportedOperationException()

        const val SIZE_BYTES: Int = 0
        const val SIZE_BITS: Int = 0
    }

    abstract operator fun plus(other: PrimitiveType): PrimitiveType
    abstract operator fun minus(other: PrimitiveType): PrimitiveType
    abstract operator fun times(other: PrimitiveType): PrimitiveType
    abstract operator fun div(other: PrimitiveType): PrimitiveType
    abstract operator fun rem(other: PrimitiveType): PrimitiveType

    abstract operator fun inc(): PrimitiveType
    abstract operator fun dec(): PrimitiveType
    abstract operator fun unaryPlus(): PrimitiveType
    abstract operator fun unaryMinus(): PrimitiveType

    abstract operator fun compareTo(other: PrimitiveType): Int

    abstract fun toPrimitive(): PrimitiveType
}

