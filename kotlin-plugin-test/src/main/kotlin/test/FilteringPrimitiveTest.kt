@file:GeneratePrimitives(DataType.BYTE, DataType.DOUBLE, DataType.FLOAT, DataType.BOOLEAN)

package test

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.*

@GenerateNameFromPrimitives
class FilteringPrimitiveTest {
    fun all(x: PrimitiveType) {
        val y = x
    }

    @FilterPrimitives(exclude = [DataType.BOOLEAN])
    fun numbers(x: PrimitiveType) {
        val y = x * x
    }
}
