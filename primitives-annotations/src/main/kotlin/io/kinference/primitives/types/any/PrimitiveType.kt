@file:Suppress("UNUSED_PARAMETER", "unused")

package io.kinference.primitives.types.any

import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.number.PrimitiveNumberType

/**
 * Primitive type that can represent any [DataType] -- numbers as well as boolean
 */
class PrimitiveType {
    operator fun compareTo(other: PrimitiveType): Int = throw UnsupportedOperationException()
    fun toPrimitive(): PrimitiveType = throw UnsupportedOperationException()
}



