@file:Suppress("unused")

package io.kinference.primitives.types.any

import io.kinference.primitives.types.number.PrimitiveNumberType

fun Number.toPrimitive(): PrimitiveNumberType = throw UnsupportedOperationException()
fun Boolean.toPrimitive(): PrimitiveType = throw UnsupportedOperationException()
