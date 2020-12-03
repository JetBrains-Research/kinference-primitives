@file:GeneratePrimitives(DataType.BYTE, DataType.DOUBLE, DataType.FLOAT, DataType.BOOLEAN)

package test

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.any.PrimitiveType
import io.kinference.primitives.types.number.PrimitiveNumberType

@GenerateNameFromPrimitives
class FilteringPrimitiveTest {
    fun all(x: PrimitiveType) {
        val y = x
    }

    @FilterPrimitives(exclude = [DataType.BOOLEAN])
    fun numbers(x: PrimitiveNumberType) {
        val y = x * x
    }
}
