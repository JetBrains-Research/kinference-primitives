@file:GeneratePrimitives(DataType.BYTE, DataType.DOUBLE, DataType.FLOAT)
@file:Suppress("unused")

package test

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.*
import io.kinference.primitives.types.any.toPrimitive
import io.kinference.primitives.types.number.PrimitiveNumberType

@GenerateNameFromPrimitives
class NumbersPrimitiveTest {
    fun test(): PrimitiveNumberType {
        val x: PrimitiveNumberType = (0).toPrimitive()
        val y: PrimitiveNumberType = (x + x).toPrimitive()
        val z = (x * y).toPrimitive()
        return (z + z).toPrimitive()
    }

    @BindPrimitives(type1 = [DataType.FLOAT, DataType.DOUBLE])
    fun test(x: @BindPrimitives.Type1 PrimitiveNumberType) {
        val y = x
    }
}
