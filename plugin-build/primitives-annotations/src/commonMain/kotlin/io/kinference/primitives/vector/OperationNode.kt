@file:Suppress("Unused", "UnusedReceiverParameter")

package io.kinference.primitives.vector

import io.kinference.primitives.types.PrimitiveArray
import io.kinference.primitives.types.PrimitiveType
import io.kinference.primitives.types.toPrimitive
import io.kinference.primitives.vector.Add
import io.kinference.primitives.vector.BinaryOp
import io.kinference.primitives.vector.Sub
import io.kinference.primitives.vector.UnaryOp

sealed class OpNode() {
    public fun into(dest: PrimitiveArray, offset: Int, len: Int): Nothing =
        throw UnsupportedOperationException()

    public fun reduce(operation: AssociativeWrapper, len: Int): PrimitiveType =
        throw UnsupportedOperationException()

}

final class PrimitiveSlice(val src: PrimitiveArray, val offset: Int = 0) : OpNode() {}

final class Value(val value: PrimitiveType) : OpNode() {}

sealed class UnaryOp(val arg: OpNode) : OpNode() {
    constructor(arg: OpNode, mask: VecMask) : this(arg)
}

sealed class BinaryOp(val left: OpNode, val right: OpNode) : OpNode() {
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}

class IfElse(val condition: VecMask, val left: OpNode, val right: OpNode) : OpNode() {}

sealed class AssociativeWrapper(){}

class Exp(arg: OpNode): UnaryOp(arg){
    constructor(arg: OpNode, mask: VecMask) : this(arg)
}
class Abs(arg: OpNode): UnaryOp(arg){
    constructor(arg: OpNode, mask: VecMask) : this(arg)
}
class Neg(arg: OpNode): UnaryOp(arg){
    constructor(arg: OpNode, mask: VecMask) : this(arg)
}
class Log(arg: OpNode): UnaryOp(arg){
    constructor(arg: OpNode, mask: VecMask) : this(arg)
}

class Add(left: OpNode, right: OpNode): BinaryOp(left, right){
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}
class Sub(left: OpNode, right: OpNode): BinaryOp(left, right){
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}
class Mul(left: OpNode, right: OpNode): BinaryOp(left, right){
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}
class Div(left: OpNode, right: OpNode): BinaryOp(left, right){
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}
class Pow(left: OpNode, right: OpNode): BinaryOp(left, right){
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}
class Max(left: OpNode, right: OpNode): BinaryOp(left, right){
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}
class Min(left: OpNode, right: OpNode): BinaryOp(left, right){
    constructor(left: OpNode, right: OpNode, mask: VecMask) : this(left, right)
}

object ADD: AssociativeWrapper(){}
object MUL: AssociativeWrapper(){}
object MAX: AssociativeWrapper(){}

sealed class VecMask(){}

sealed class Comparator(left: OpNode, right: OpNode): VecMask(){}
sealed class MaskBinaryOp(left: VecMask, right: VecMask): VecMask(){}
sealed class MaskUnaryOp(arg: VecMask): VecMask(){}

class Not(arg: VecMask): VecMask(){}

class Eq(left: OpNode, right: OpNode): Comparator(left, right){}
class Neq(left: OpNode, right: OpNode): Comparator(left, right){}
class LT(left: OpNode, right: OpNode): Comparator(left, right){}
class LE(left: OpNode, right: OpNode): Comparator(left, right){}
class GT(left: OpNode, right: OpNode): Comparator(left, right){}
class GE(left: OpNode, right: OpNode): Comparator(left, right){}

class And(left: VecMask, right: VecMask): MaskBinaryOp(left, right){}
class Or(left: VecMask, right: VecMask): MaskBinaryOp(left, right){}
class Xor(left: VecMask, right: VecMask): MaskBinaryOp(left, right){}
