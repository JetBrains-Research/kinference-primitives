package io.kinference.primitives.handler

import io.kinference.primitives.types.DataType
import java.util.*
import kotlin.reflect.KClass

class Primitive<Type : Any, ArrayType : Any>(val dataType: DataType, type: KClass<Type>, arrayType: KClass<ArrayType>) {
    companion object {
        private val ALL = EnumMap<DataType, Primitive<*, *>>(DataType::class.java)

        private inline fun <reified Type : Any, reified ArrayType : Any> create(type: DataType) {
            require(type !in ALL) { "DataType $type already registered" }
            ALL[type] = Primitive(type, Type::class, ArrayType::class)
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

            create<Boolean, BooleanArray>(DataType.BOOLEAN)
        }

        fun all() = ALL.values
        fun of(type: DataType) = ALL[type] ?: error("DataType $type not registered")
    }

    val typeName = type.simpleName!!
    val arrayTypeName = arrayType.simpleName!!
}

