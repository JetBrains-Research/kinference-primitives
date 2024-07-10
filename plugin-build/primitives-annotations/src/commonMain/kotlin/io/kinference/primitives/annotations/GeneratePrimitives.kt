package io.kinference.primitives.annotations

import io.kinference.primitives.types.*

/** Specify that any usage of [PrimitiveNumberType] or [PrimitiveArray] is subject for replacement in this file */
@Target(AnnotationTarget.FILE)
annotation class GeneratePrimitives(vararg val types: DataType)
