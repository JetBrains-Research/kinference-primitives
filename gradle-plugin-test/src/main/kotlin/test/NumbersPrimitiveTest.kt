@file:GeneratePrimitives(DataType.BYTE, DataType.DOUBLE, DataType.FLOAT)
@file:Suppress("unused")

package test

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.*
import io.kinference.primitives.types.toPrimitive

@GenerateNameFromPrimitives
class NumbersPrimitiveTest {
    fun test(): PrimitiveType {
        val x: PrimitiveType = (0).toPrimitive()
        val y: PrimitiveType = (x + x).toPrimitive()
        val z = (x * y).toPrimitive()
        return (z + z).toPrimitive()
    }

    @BindPrimitives(type1 = [DataType.FLOAT, DataType.DOUBLE])
    fun test(x: @BindPrimitives.Type1 PrimitiveType) {
        val y = x
    }
}
