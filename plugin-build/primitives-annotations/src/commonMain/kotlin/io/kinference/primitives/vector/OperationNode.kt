@file:Suppress("Unused", "UnusedReceiverParameter")

package io.kinference.primitives.vector

import io.kinference.primitives.types.PrimitiveArray
import io.kinference.primitives.types.PrimitiveType
import io.kinference.primitives.types.toPrimitive

sealed class OpNode() {
    public fun into(dest: PrimitiveArray, offset: Int, len: Int): Nothing =
        throw UnsupportedOperationException()

    public fun reduce(operation: AssociativeWrapper, len: Int): PrimitiveType =
        throw UnsupportedOperationException()

}

final class PrimitiveSlice(val src: PrimitiveArray, val offset: Int = 0) : OpNode() {}

final class Value(val value: PrimitiveType) : OpNode() {}

sealed class UnaryOp(val arg: OpNode) : OpNode() {}

sealed class BinaryOp(val left: OpNode, val right: OpNode) : OpNode() {}

sealed class AssociativeWrapper(){}

class Exp(arg: OpNode): UnaryOp(arg){}

class Add(left: OpNode, right: OpNode): BinaryOp(left, right){}
class Neg(arg: OpNode): UnaryOp(arg){}
class Log(arg: OpNode): UnaryOp(arg){}

class Sub(left: OpNode, right: OpNode): BinaryOp(left, right){}
class Mul(left: OpNode, right: OpNode): BinaryOp(left, right){}
class Div(left: OpNode, right: OpNode): BinaryOp(left, right){}
class Pow(left: OpNode, right: OpNode): BinaryOp(left, right){}
class Max(left: OpNode, right: OpNode): BinaryOp(left, right){}
class Min(left: OpNode, right: OpNode): BinaryOp(left, right){}

object ADD: AssociativeWrapper(){}
object MUL: AssociativeWrapper(){}
object MAX: AssociativeWrapper(){}

fun main(){
}
