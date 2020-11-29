package io.kinference.primitives.handler

import io.kinference.primitives.types.DataType

fun DataType.toPrimitive(): Primitive<*, *> = Primitive.of(this)
