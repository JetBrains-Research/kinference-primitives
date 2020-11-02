@file:GenerateWithPrimitives

package test

import io.kinference.primitives.annotations.GenerateWithPrimitives
import io.kinference.primitives.annotations.PrimitiveClass
import io.kinference.primitives.types.PrimitiveType
import io.kinference.primitives.types.toPrimitive

@PrimitiveClass
class PrimitiveTest {
    fun test(): PrimitiveType {
        val x: PrimitiveType = (0).toPrimitive()
        return x
    }
}
