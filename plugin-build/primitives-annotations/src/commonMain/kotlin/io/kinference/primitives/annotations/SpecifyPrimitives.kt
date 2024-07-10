package io.kinference.primitives.annotations

import io.kinference.primitives.types.DataType

/**
 * Specify primitives for which this function or class would be generated
 *
 * For example, you can specify [DataType.DOUBLE] if some operations expects
 * that PrimitiveType is Double type number
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class SpecifyPrimitives(
    val include: Array<DataType> = arrayOf(DataType.ALL)
)
