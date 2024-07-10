@file:Suppress("unused", "UndocumentedPublicProperty", "UndocumentedPublicFunction")

package io.kinference.primitives.types

/**
 * Representation of primitive type (like [Int] or [Boolean]) that
 * is used in code that should be specialized.
 *
 * So, basically [PrimitiveType] should be used as a substitution of some specific
 * primitive type in code where you'd like to generate primitive specializations.
 *
 * Consider it some kind of C++ template variable.
 */
abstract class PrimitiveType {
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

    abstract fun toByte(): Byte
    abstract fun toShort(): Short
    abstract fun toInt(): Int
    abstract fun toLong(): Long
    abstract fun toDouble(): Double
    abstract fun toFloat(): Float

    abstract fun toPrimitive(): PrimitiveType
}

