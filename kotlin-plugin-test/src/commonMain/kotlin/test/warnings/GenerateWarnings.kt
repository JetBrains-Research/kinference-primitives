@file:GeneratePrimitives
package test.warnings

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.PrimitiveType

@GenerateNameFromPrimitives
fun myGeneratePrimitiveFunction(a: @BindPrimitives.Type1 PrimitiveType) {

}
