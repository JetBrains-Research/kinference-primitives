@file:GeneratePrimitives
package test.warnings

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.DataType
import javax.lang.model.type.PrimitiveType

@GenerateNameFromPrimitives
@BindPrimitives
fun myPrimitiveFunction(a: @BindPrimitives.Type1 PrimitiveType) {

}
