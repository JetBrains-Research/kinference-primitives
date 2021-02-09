@file:GeneratePrimitives(DataType.ALL)
package test

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.types.DataType

@GenerateNameFromPrimitives
class DataTypeCurrentPrimitive {
    val type = DataType.CurrentPrimitive
}
