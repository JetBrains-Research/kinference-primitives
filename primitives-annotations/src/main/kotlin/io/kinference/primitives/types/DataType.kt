package io.kinference.primitives.types

/**
 * Types of primitives that can be created during generation.
 *
 * Note, that unsigned values requires special care, since right now all of
 * them are considered experimental.
 *
 * For example, if you use unsigned values you have to bundle of the version
 * with which project was compiled stdlib.
 */
enum class DataType {
    BYTE,
    SHORT,
    INT,
    LONG,

    UBYTE,
    USHORT,
    UINT,
    ULONG,

    FLOAT,
    DOUBLE,

    BOOLEAN,

    ALL,
    NUMBER,

    UNKNOWN;

    fun resolve(): Set<DataType> {
        return when(this) {
            ALL -> setOf(BYTE, SHORT, INT, LONG, UBYTE, USHORT, UINT, ULONG, FLOAT, DOUBLE, BOOLEAN)
            NUMBER -> setOf(BYTE, SHORT, INT, LONG, UBYTE, USHORT, UINT, ULONG, FLOAT, DOUBLE)
            else -> setOf(this)
        }
    }
}
