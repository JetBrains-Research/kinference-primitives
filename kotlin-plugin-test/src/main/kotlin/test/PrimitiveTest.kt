@file:GenerateWithPrimitives

package test

import io.kinference.primitives.annotations.Exclude
import io.kinference.primitives.annotations.GenerateWithPrimitives
import io.kinference.primitives.annotations.PrimitiveClass
import io.kinference.primitives.types.*

@PrimitiveClass
@Exclude([DataType.BOOLEAN])
class PrimitiveTest {
    fun test(): PrimitiveType {
        val x: PrimitiveType = (0).toPrimitive()
        val y: PrimitiveType = (x + x).toPrimitive()
        val z = (y * y).toPrimitive()
        val mem = (z + z).toPrimitive()
        return y
    }
}
