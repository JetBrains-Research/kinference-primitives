@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.kinference.primitives.types

/**
 * Types of primitives that can be created during generation.
 *
 * There are two special groups - [NUMBER] and [ALL], first includes
 * all number types, second includes [NUMBER] and [BOOLEAN]
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
    VECTORIZABLE;

    /**
     * Resolve DataType into actual primitives -- would flatten groups into collection of primitives.
     * Primitives would remain the same
     */
    fun resolve(): Set<DataType> {
        return when (this) {
            ALL -> setOf(BYTE, SHORT, INT, LONG, UBYTE, USHORT, UINT, ULONG, FLOAT, DOUBLE, BOOLEAN)
            NUMBER -> setOf(BYTE, SHORT, INT, LONG, UBYTE, USHORT, UINT, ULONG, FLOAT, DOUBLE)
            VECTORIZABLE -> setOf(BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)
            else -> setOf(this)
        }
    }

    companion object {
        /**
         * Use this field to specify DataType with which file would be specialized.
         *
         * For example, if file is generated for [Boolean] then [DataType.CurrentPrimitive]
         * would be replaced with [DataType.BOOLEAN]
         */
        val CurrentPrimitive: DataType
            get() = error("This field should never be accessed in runtime")
    }
}
