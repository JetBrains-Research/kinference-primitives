@file:GeneratePrimitives(DataType.ALL)

package test.cross

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.DataType

@GenerateNameFromPrimitives
@MakePublic
internal class PrimitiveFirst {
    val second = PrimitiveSecond()
}
