package io.kinference.primitives.tasks

import io.kinference.primitives.types.DataType
import kotlin.reflect.KClass

class Primitive<Type : Any, ArrayType : Any>(val dataType: DataType, type: KClass<Type>, arrayType: KClass<ArrayType>) {
    companion object {
        inline fun <reified Type : Any, reified ArrayType : Any> create(dataType: DataType): Primitive<Type, ArrayType> {
            return Primitive(dataType, Type::class, ArrayType::class)
        }
    }

    val typeName = type.simpleName
    val arrayTypeName = arrayType.simpleName
}