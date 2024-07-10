@file:Suppress("Unused")
@file:GeneratePrimitives(DataType.BYTE, DataType.DOUBLE, DataType.FLOAT, DataType.BOOLEAN)

package test

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.*

@GenerateNameFromPrimitives
@MakePublic
internal class FilteringPrimitiveTest {
    fun all(x: PrimitiveType) : PrimitiveType {
        val y = x
        return y
    }

    @FilterPrimitives(exclude = [DataType.BOOLEAN])
    fun numbers(x: PrimitiveType): PrimitiveType {
        val y = x * x
        return y.toPrimitive()
    }
}
