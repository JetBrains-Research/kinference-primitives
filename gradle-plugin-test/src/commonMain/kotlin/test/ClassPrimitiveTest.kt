@file:GeneratePrimitives(DataType.FLOAT, DataType.INT)
package test

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.*

@GenerateNameFromPrimitives
@MakePublic
internal class ClassPrimitiveTest {
    val a = PrimitiveType.MIN_VALUE

    companion object {
        val x: PrimitiveType = PrimitiveType.MAX_VALUE
        val y = ClassPrimitiveTest.x
        val z = ClassPrimitiveTest()
    }
}

internal fun ClassPrimitiveTest.v() = Unit


