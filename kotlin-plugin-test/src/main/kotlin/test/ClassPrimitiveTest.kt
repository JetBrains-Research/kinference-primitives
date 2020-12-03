@file:GeneratePrimitives(DataType.FLOAT, DataType.INT)
package test

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.number.PrimitiveNumberType

@GenerateNameFromPrimitives
class ClassPrimitiveTest {
    val a = PrimitiveNumberType.MIN_VALUE

    companion object {
        val x: PrimitiveNumberType = PrimitiveNumberType.MAX_VALUE
        val y = ClassPrimitiveTest.x
        val z = ClassPrimitiveTest()
    }
}

fun ClassPrimitiveTest.v() = Unit


