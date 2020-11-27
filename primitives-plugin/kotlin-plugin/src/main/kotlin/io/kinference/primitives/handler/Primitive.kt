package io.kinference.primitives.handler

import io.kinference.primitives.types.DataType
import java.util.*
import kotlin.reflect.KClass


@ExperimentalUnsignedTypes
class Primitive<Type : Any, ArrayType : Any>(val dataType: DataType, type: KClass<Type>, arrayType: KClass<ArrayType>) {
    companion object {
        private val ALL: MutableMap<DataType, Primitive<*, *>> = EnumMap(DataType::class.java)

        private inline fun <reified Type : Any, reified ArrayType : Any> create(dataType: DataType): Primitive<Type, ArrayType> {
            require(!ALL.contains(dataType)) { "DataType already registered" }
            return Primitive(dataType, Type::class, ArrayType::class).apply { ALL[dataType] = this }
        }

        init {
            create<Byte, ByteArray>(DataType.BYTE)
            create<Short, ShortArray>(DataType.SHORT)
            create<Int, IntArray>(DataType.INT)
            create<Long, LongArray>(DataType.LONG)

            create<UByte, UByteArray>(DataType.UBYTE)
            create<UShort, UShortArray>(DataType.USHORT)
            create<UInt, UIntArray>(DataType.UINT)
            create<ULong, ULongArray>(DataType.ULONG)

            create<Float, FloatArray>(DataType.FLOAT)
            create<Double, DoubleArray>(DataType.DOUBLE)

//            create<Boolean, BooleanArray>(DataType.BOOLEAN)
        }

        fun all() = ALL.values
        fun of(type: DataType) = ALL[type] ?: throw IllegalStateException("DataType not registered")
    }

    val typeName = type.simpleName!!
    val arrayTypeName = arrayType.simpleName!!
}

@ExperimentalUnsignedTypes
fun DataType.toPrimitive(): Primitive<*, *> = Primitive.of(this)
