@file:GeneratePrimitives(DataType.FLOAT, DataType.INT)
package test

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.types.*

@GenerateNameFromPrimitives
class ClassPrimitiveTest {
    val a = PrimitiveType.MIN_VALUE

    companion object {
        val x: PrimitiveType = PrimitiveType.MAX_VALUE
        val y = ClassPrimitiveTest.x
        val z = ClassPrimitiveTest()
    }
}

fun ClassPrimitiveTest.v() = Unit


