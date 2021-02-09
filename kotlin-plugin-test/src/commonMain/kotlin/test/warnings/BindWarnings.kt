@file:GeneratePrimitives(DataType.ALL)
package test.warnings

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.PrimitiveType

@GenerateNameFromPrimitives
@BindPrimitives
fun myPrimitiveFunction(a: @BindPrimitives.Type1 PrimitiveType) {

}
