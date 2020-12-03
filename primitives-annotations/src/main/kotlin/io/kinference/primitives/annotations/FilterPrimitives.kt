package io.kinference.primitives.annotations

import io.kinference.primitives.types.DataType

/**
 * Filter primitives for which this function or class would be generated
 *
 * With this annotation you can filter out some functions, that are not
 * applicable for all types used in this file.
 *
 * For example, you can exclude [DataType.BOOLEAN] if some operations expects
 * that PrimitiveType is Number
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class FilterPrimitives(
    val exclude: Array<DataType> = []
)
