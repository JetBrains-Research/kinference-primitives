@file:Suppress("unused", "UNUSED_PARAMETER")

package io.kinference.primitives.types.number

import io.kinference.primitives.types.any.PrimitiveType

/**
 * Primitive type that can represent only number types
 */
class PrimitiveNumberType: Number() {
    companion object {
        val MIN_VALUE: PrimitiveNumberType = PrimitiveNumberType()
        val MAX_VALUE: PrimitiveNumberType = PrimitiveNumberType()

        const val SIZE_BYTES: Int = 0
        const val SIZE_BITS: Int = 0
    }

    init {
        error("Don't use this class in runtime")
    }

    operator fun plus(other: PrimitiveNumberType): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun minus(other: PrimitiveNumberType): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun times(other: PrimitiveNumberType): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun div(other: PrimitiveNumberType): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun rem(other: PrimitiveNumberType): PrimitiveNumberType = throw UnsupportedOperationException()

    operator fun inc(): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun dec(): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun unaryPlus(): PrimitiveNumberType = throw UnsupportedOperationException()
    operator fun unaryMinus(): PrimitiveNumberType = throw UnsupportedOperationException()

    operator fun compareTo(other: PrimitiveType): Int = throw UnsupportedOperationException()

    fun toPrimitive(): PrimitiveNumberType = throw UnsupportedOperationException()
    override fun toByte(): Byte = throw UnsupportedOperationException()
    override fun toChar(): Char = throw UnsupportedOperationException()
    override fun toDouble(): Double = throw UnsupportedOperationException()
    override fun toFloat(): Float = throw UnsupportedOperationException()
    override fun toInt(): Int = throw UnsupportedOperationException()
    override fun toLong(): Long = throw UnsupportedOperationException()
    override fun toShort(): Short = throw UnsupportedOperationException()
}

