@file:GeneratePrimitives(DataType.ALL)

package test.cross

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.DataType

@GenerateNameFromPrimitives
@MakePublic
internal class PrimitiveSecond {
    val first = PrimitiveFirst()
}
