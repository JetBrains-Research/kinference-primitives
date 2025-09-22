package io.kinference.primitives.annotations

import io.kinference.primitives.types.*
import io.kinference.primitives.vector.*

/** Specify that any usage of [OperationNode] is subject for replacement in this file */
@Target(AnnotationTarget.FILE)
annotation class GenerateVector(vararg val types: DataType)
