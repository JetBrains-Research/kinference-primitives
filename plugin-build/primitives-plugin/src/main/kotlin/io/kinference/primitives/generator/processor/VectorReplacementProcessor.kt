package io.kinference.primitives.generator.processor

import io.kinference.primitives.generator.Primitive
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

// TODO: merge linear and vector replacement functions, this is quadratic in op tree size

internal class VectorReplacementProcessor(val primitive: Primitive<*, *>) {
    val vecName = "${primitive.typeName}Vector"
    val vecSpecies = "$vecName.SPECIES_PREFERRED"
    val vecLen = "$vecName.length()"

    companion object {
        val unaryLinearReplacements = mapOf(
            "Exp" to { x: String -> "exp($x)" },
            "Abs" to { x: String -> "abs($x)" },
            "Neg" to { x: String -> "(-$x)" },
            "Log" to { x: String -> "ln($x)" },
        ).withDefault { null }

        val binaryLinearReplacements = mapOf(
            "Add" to { x: String, y: String -> "($x + $y)" },
            "Sub" to { x: String, y: String -> "($x - $y)" },
            "Mul" to { x: String, y: String -> "($x * $y)" },
            "Div" to { x: String, y: String -> "($x / $y)" },
            "Max" to { x: String, y: String -> "max($x, $y)" },
            "Min" to { x: String, y: String -> "min($x, $y)" },
            "Pow" to { x: String, y: String -> "($x).pow($y)" },
        ).withDefault { null }

        val isAssoc = mapOf<String, Boolean>(
            "ADD" to true,
            "MUL" to true,
            "MIN" to true,
            "MAX" to true,
        ).withDefault { false }

        val isCommutative = mapOf<String, Boolean>(
            "ADD" to true,
            "MUL" to true,
            "MIN" to true,
            "MAX" to true,
        ).withDefault { false }

        val vectorHandles = mapOf(
            "Add" to "ADD",
            "Sub" to "SUB",
            "Mul" to "MUL",
            "Div" to "DIV",
            "Exp" to "EXP",
            "Max" to "MAX",
            "Min" to "MIN",
            "Abs" to "ABS",
            "Log" to "LOG",
            "Neg" to "NEG",
            "Pow" to "POW"
        ).withDefault { null }

    }

    val neutralElement = mapOf(
        "ADD" to "0.${ReplacementProcessor.toType(primitive)}()",
        "MUL" to "1.${ReplacementProcessor.toType(primitive)}()",
        "MIN" to "${primitive.typeName}.MAX_VALUE",
        "MAX" to "${primitive.typeName}.MIN_VALUE"
    ).withDefault { null }

    fun process(expr: KtExpression?, collector: MessageCollector): Triple<String, String, Boolean>? {
        if (expr == null) return null
        if (expr !is KtCallExpression) return null
        val args = expr.valueArguments
        val name = expr.calleeExpression?.text ?: return null
        return when (name) {
            "UnaryOp" -> {
                if (args.size != 2) return null
                val childExpr = args[0].getArgumentExpression()
                val (childVector, childLinear, isValue) = process(childExpr, collector) ?: return null
                val handle = vectorHandles[args[1].text] ?: return null
                val linReplace = unaryLinearReplacements[args[1].text] ?: return null
                Triple(
                    """$childVector
                    .lanewise(VectorOperators.$handle)""".trimIndent(),
                    linReplace(childLinear),
                    isValue
                )
            }

            "BinaryOp" -> {
                if (args.size != 3) return null
                val leftExpr = args[0].getArgumentExpression() ?: return null
                val rightExpr = args[1].getArgumentExpression() ?: return null
                val (leftVector, leftLinear, leftValue) = process(leftExpr, collector) ?: return null
                val (rightVector, rightLinear, rightValue) = process(rightExpr, collector) ?: return null
                val isValue = leftValue && rightValue
                val handle = vectorHandles[args[2].text] ?: return null
                val linear = binaryLinearReplacements[args[2].text]?.invoke(leftLinear, rightLinear) ?: return null
                Triple(
                    if (isValue)
                        linear
                    else if (rightValue)
                        """$leftVector.
                        lanewise(VectorOperators.$handle, $rightLinear)""".trimIndent()
                    else if (leftValue && isCommutative[handle] == true)
                        """$rightVector.lanewise(VectorOperators.$handle, $leftLinear)"""
                    else if (leftValue)
                        """$vecName.broadcast($vecSpecies, $leftLinear).lanewise(VectorOperators.$handle, $rightVector)"""
                    else
                        """$leftVector
                    .lanewise(VectorOperators.$handle, $rightVector)""".trimIndent(),
                    linear,
                    isValue
                )
            }

            "Value" -> {
                if (args.size != 1) return null
                val replacement = "${args[0].text}"
                Triple(replacement, replacement, true)
            }

            "PrimitiveSlice" -> {
                if (args.size != 2 && args.size != 1) return null
                val src = args[0].text
                val offset = when (args.size) {
                    2 -> args[1].text
                    else -> "0"
                }
                Triple(
                    "${vecName}.fromArray($vecSpecies, $src, $offset + _vec_internal_idx)",
                    "$src[$offset + _vec_internal_idx]",
                    false
                )
            }

            else -> null
        }
    }
}
