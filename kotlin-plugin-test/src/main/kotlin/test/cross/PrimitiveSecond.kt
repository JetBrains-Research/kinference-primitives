@file:GeneratePrimitives(DataType.ALL)

package test.cross

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.types.DataType

@GenerateNameFromPrimitives
class PrimitiveSecond {
    val first = PrimitiveFirst()
}
