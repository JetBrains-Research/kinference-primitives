package io.kinference.primitives.annotations

import io.kinference.primitives.types.DataType

/** Specify that in this class or function `Primitive` substring in name should be replaced with currently generated [DataType] */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class GenerateNameFromPrimitives
