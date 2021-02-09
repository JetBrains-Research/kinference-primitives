package io.kinference.primitives.annotations

import io.kinference.primitives.types.DataType

/**
 * With BindPrimitives annotation you can create cross-products of types. Appropriate
 * code would be generated for each pair/triple/quadruple
 *
 * For example, you can use this annotation to generate `apply` function that will
 * apply any of the types at [type1] on any type generated in this file.
 *
 * @param type1 to specify [DataType] which type parameters annotated with [BindPrimitives.Type1]
 * would present
 * @param type2 to specify [DataType] which type parameters annotated with [BindPrimitives.Type2]
 * would present
 * @param type3 to specify [DataType] which type parameters annotated with [BindPrimitives.Type3]
 * would present
 */
@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class BindPrimitives(
    val type1: Array<DataType> = emptyArray(),
    val type2: Array<DataType> = emptyArray(),
    val type3: Array<DataType> = emptyArray()
) {
    /**
     * Makes possible wiring of [DataType] from [type1] parameter
     * to some specific type parameters in function
     */
    @Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type1


    /**
     * Makes possible wiring of [DataType] from [type2] parameter
     * to some specific type parameters in function
     */
    @Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type2

    /**
     * Makes possible wiring of [DataType] from [type3] parameter
     * to some specific type parameters in function
     */
    @Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type3
}
