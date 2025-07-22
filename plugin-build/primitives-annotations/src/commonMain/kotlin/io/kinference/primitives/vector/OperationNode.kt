@file:Suppress("Unused", "UnusedReceiverParameter")

package io.kinference.primitives.vector

import io.kinference.primitives.types.PrimitiveArray
import io.kinference.primitives.types.PrimitiveType

sealed class OpNode() {
    public fun into(dest: PrimitiveArray, offset: Int, len: Int) {
        throw UnsupportedOperationException()
    }
    public fun reduce(operation: AssociativeWrapper, len: Int): PrimitiveType {
        throw UnsupportedOperationException()
    }

    internal abstract val isValue: Boolean
    //internal abstract fun linReplace(): String
    //internal abstract fun vecReplace(): String
}

class PrimitiveSlice(val src: PrimitiveArray, val offset: Int = 0) : OpNode() {
    override val isValue: Boolean = false
}

class UnaryOp(val arg: OpNode, val operation: UnaryWrapper) : OpNode() {
    override val isValue: Boolean = arg.isValue
}

class BinaryOp(val left: OpNode, val right: OpNode, val operation: BinaryWrapper) : OpNode() {
    override val isValue: Boolean = left.isValue && right.isValue
}

class Value(val value: PrimitiveType) : OpNode() {
    override val isValue: Boolean = true
}

sealed class UnaryWrapper() {
}

sealed class BinaryWrapper() {
}

sealed class AssociativeWrapper() : BinaryWrapper() {
}

object Abs : UnaryWrapper() {}

object Exp : UnaryWrapper() {}

object Log : UnaryWrapper() {}

object Neg : UnaryWrapper() {}

object Add : AssociativeWrapper() {}

object Sub : BinaryWrapper() {}

object Mul : AssociativeWrapper() {}

object Div : BinaryWrapper() {}

object Pow : BinaryWrapper() {}

object Max : AssociativeWrapper() {}

object Min : BinaryWrapper() {}
