package io.kinference.primitives.generator.processor

import io.kinference.primitives.generator.Primitive
import io.kinference.primitives.vector.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.typeUtil.supertypes

// TODO: merge linear and vector replacement functions, this is quadratic in op tree size

internal class VectorReplacementProcessor(private val context: BindingContext, val primitive: Primitive<*, *>) {
    val vecName = "${primitive.typeName}Vector"
    val vecSpecies = "$vecName.SPECIES_PREFERRED"
    val vecLen = "$vecName.length()"

    companion object {
        val unaryLinearReplacements = mapOf(
            "EXP" to { x: String -> "exp($x)" },
            "ABS" to { x: String -> "abs($x)" },
            "NEG" to { x: String -> "(-$x)" },
            "LOG" to { x: String -> "ln($x)" },
        ).withDefault { null }

        val binaryLinearReplacements = mapOf(
            "ADD" to { x: String, y: String -> "($x + $y)" },
            "SUB" to { x: String, y: String -> "($x - $y)" },
            "MUL" to { x: String, y: String -> "($x * $y)" },
            "DIV" to { x: String, y: String -> "($x / $y)" },
            "MAX" to { x: String, y: String -> "max($x, $y)" },
            "MIN" to { x: String, y: String -> "min($x, $y)" },
            "POW" to { x: String, y: String -> "($x).pow($y)" },
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

        val opNodeTypename = OpNode::class.qualifiedName
        val unaryOpNames = UnaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val binaryOpNames = BinaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val valueType = Value::class.qualifiedName
        val primitiveSliceType = PrimitiveSlice::class.qualifiedName
        val associativeWrapperType = AssociativeWrapper::class.qualifiedName
    }

    val neutralElement = mapOf(
        "ADD" to "0.${ReplacementProcessor.toType(primitive)}()",
        "MUL" to "1.${ReplacementProcessor.toType(primitive)}()",
        "MIN" to "${primitive.typeName}.MAX_VALUE",
        "MAX" to "${primitive.typeName}.MIN_VALUE"
    ).withDefault { null }

    fun process(expr: KtExpression?, collector: MessageCollector): Triple<String, String, Boolean>? {
        if (expr == null) return null
        val exprType = context.getType(expr) ?: return null
        val exprTypename = exprType.getKotlinTypeFqName(false)
        val shortName = exprTypename.substringAfterLast('.')
        if (expr !is KtCallExpression) return null
        val args = expr.valueArguments
        return when {
            exprTypename in unaryOpNames -> {
                if (args.size != 1) return null
                val childExpr = args[0].getArgumentExpression()
                val (childVector, childLinear, isValue) = process(childExpr, collector) ?: return null
                val handle = vectorHandles[shortName] ?: return null
                val linReplace = unaryLinearReplacements[handle] ?: return null
                Triple(
                    """$childVector
                    .lanewise(VectorOperators.$handle)""".trimIndent(),
                    linReplace(childLinear),
                    isValue
                )
            }

            exprTypename in binaryOpNames -> {
                if (args.size != 2) return null
                val leftExpr = args[0].getArgumentExpression() ?: return null
                val rightExpr = args[1].getArgumentExpression() ?: return null
                val (leftVector, leftLinear, leftValue) = process(leftExpr, collector) ?: return null
                val (rightVector, rightLinear, rightValue) = process(rightExpr, collector) ?: return null
                val isValue = leftValue && rightValue
                val handle = vectorHandles[shortName] ?: return null
                val linear = binaryLinearReplacements[handle]?.invoke(leftLinear, rightLinear) ?: return null
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

            exprTypename == valueType -> {
                if (args.size != 1) return null
                val replacement = "${args[0].text}"
                Triple(replacement, replacement, true)
            }

            exprTypename == primitiveSliceType -> {
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
