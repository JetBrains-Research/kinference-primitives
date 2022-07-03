@file:GeneratePrimitives(DataType.BYTE, DataType.DOUBLE, DataType.FLOAT, DataType.BOOLEAN)

package test

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.*

@GenerateNameFromPrimitives
class SpecifyPrimitivesTest {
    fun all(x: PrimitiveType): PrimitiveType {
        val y = x
        return y
    }

    @SpecifyPrimitives(include = [DataType.FLOAT, DataType.DOUBLE])
    fun floats(x: PrimitiveType): PrimitiveType {
        val y = x * x
        return y.toPrimitive()
    }
}
