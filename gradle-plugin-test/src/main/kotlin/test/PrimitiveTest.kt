@file:GenerateWithPrimitives

package test

import io.kinference.primitives.annotations.Exclude
import io.kinference.primitives.annotations.GenerateWithPrimitives
import io.kinference.primitives.annotations.PrimitiveClass
import io.kinference.primitives.types.*

@PrimitiveClass
@Exclude([DataType.BOOLEAN])
class PrimitiveTest {
    fun test(): Int {
        val z1: PrimitiveType = (0).toPrimitive()
        val y: PrimitiveType = (z1 + z1).toPrimitive()
        val z = (y * y).toPrimitive()
        val mem = (z + z).toPrimitive()
        return mem
    }
}
