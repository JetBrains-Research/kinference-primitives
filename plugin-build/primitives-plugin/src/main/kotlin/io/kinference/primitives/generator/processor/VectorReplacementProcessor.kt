package io.kinference.primitives.generator.processor

import io.kinference.primitives.generator.Primitive
import io.kinference.primitives.vector.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.resolve.source.getPsi


internal class VectorReplacementProcessor(private val context: BindingContext, val primitive: Primitive<*, *>) {
    val vecName = "${primitive.typeName}Vector"
    val vecSpecies = "$vecName.SPECIES_PREFERRED"
    val vecLen = "$vecName.length()"

    companion object {
        val unaryLinearReplacements = mapOf(
            "EXP" to { x: String -> "FastMath.exp($x)" },
            "ABS" to { x: String -> "abs($x)" },
            "NEG" to { x: String -> "(-$x)" },
            "LOG" to { x: String -> "ln($x)" },
        ).withDefault { null }

        val binaryLinearReplacements = mapOf(
            "ADD" to { x: String, y: String -> "($x + $y)" },
            "SUB" to { x: String, y: String -> "($x - $y)" },
            "MUL" to { x: String, y: String -> "($x * $y)" },
            "DIV" to { x: String, y: String -> "($x / $y)" },
            "MAX" to { x: String, y: String -> "maxOf($x, $y)" },
            "MIN" to { x: String, y: String -> "minOf($x, $y)" },
            "POW" to { x: String, y: String -> "($x).pow($y)" },
        ).withDefault { null }

        val isAssoc = mapOf<String, Boolean>(
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
            "Pow" to "POW",
        ).withDefault { null }

        val maskHandles = mapOf(
            "And" to "AND", "Or" to "OR", "Xor" to "XOR", "Not" to "NOT", "Eq" to "EQ", "Neq" to "NEQ", "LT" to "LT", "LE" to "LE", "GT" to "GT", "GE" to "GE"
        ).withDefault { null }

        val maskUnaryReplacement = mapOf(
            "Not" to ({ x: String -> "$x.not()" }),
        ).withDefault { null }

        val maskBinaryReplacement = mapOf(
            "And" to ({ x: String, y: String -> "($x.and($y)" }),
            "Or" to ({ x: String, y: String -> "$x.or($y)" }),
            "Xor" to ({ x: String, y: String -> "$x.xor($y)" }),
        ).withDefault { null }

        val comparatorReplacement = mapOf(
            "Eq" to ({ x: String, y: String -> "($x == $y)" }),
            "Neq" to ({ x: String, y: String -> "($x != $y)" }),
            "LT" to ({ x: String, y: String -> "($x < $y)" }),
            "LE" to ({ x: String, y: String -> "($x <= $y)" }),
            "GT" to ({ x: String, y: String -> "($x > $y)" }),
            "GE" to ({ x: String, y: String -> "($x >= $y)" }),
        )

        val opNodeTypename = OpNode::class.qualifiedName
        val opNodeTypes = OpNode::class.sealedSubclasses.map { it.qualifiedName }
        val unaryOpNames = UnaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val binaryOpNames = BinaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val valueType = Value::class.qualifiedName
        val primitiveSliceType = PrimitiveSlice::class.qualifiedName
        val associativeWrapperType = AssociativeWrapper::class.qualifiedName
        val maskTypes = VecMask::class.sealedSubclasses.map { it.qualifiedName }
        val maskBinaryOpTypes = MaskBinaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val maskUnaryOpTypes = MaskUnaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val comparatorTypes = Comparator::class.sealedSubclasses.map { it.qualifiedName }
        val ifElseType = IfElse::class.qualifiedName
    }

    val neutralElement = mapOf(
        "ADD" to "0.${ReplacementProcessor.toType(primitive)}()",
        "MUL" to "1.${ReplacementProcessor.toType(primitive)}()",
        "MIN" to "${primitive.typeName}.MAX_VALUE",
        "MAX" to "${primitive.typeName}.MIN_VALUE"
    ).withDefault { null }

    var valueDeclarations: String = ""
    var vecDeclarations: String = ""
    var linDeclarations: String = ""
    var localVariables: Set<String> = emptySet()

    fun processDeclaration(expr: KtExpression, collector: MessageCollector): Triple<String, String, Boolean>? {
        if (expr !is KtSimpleNameExpression) return null
        val varName = expr.text

        val descriptor = context.get(BindingContext.REFERENCE_TARGET, expr) ?: return null //Triple("NOT_DECL: ${expr.text}", "NOT_DECL", false)
        val declaration = descriptor.toSourceElement.getPsi() ?: return null //Triple("NOT_DECL_PSI: ${expr.text}", "NOT_DECL", false)

        if (declaration !is KtVariableDeclaration) return null
        val actualBody = declaration.initializer ?: return null //Triple("NOT_DECL_BODY: ${expr.text}", "NOT_DECL", false)
        //return Triple("BODY: ${actualBody.text}", "", false)
        val (vecReplacement, linReplacement, value) = process(actualBody, collector) ?: return null
        if (varName !in localVariables) {
            localVariables = localVariables + varName
            if (value) {
                valueDeclarations += "val ${varName}_vec = $vecReplacement\n"
                valueDeclarations += "val ${varName}_lin = $linReplacement\n"
            } else {
                vecDeclarations += "val ${varName}_vec = $vecReplacement\n"
                linDeclarations += "val ${varName}_lin = $linReplacement\n"
            }
        }
        return Triple("${varName}_vec", "${varName}_lin", value)
    }

    fun process(expr: KtExpression?, collector: MessageCollector): Triple<String, String, Boolean>? {
        if (expr == null) return null
        if (expr !is KtCallExpression) {
            return processDeclaration(expr, collector)
        }
        val exprType = context.getType(expr) ?: return Triple("NOT_TYPED", "NOT_TYPED", false)
        val exprTypename = exprType.getKotlinTypeFqName(false)
        val shortName = exprTypename.substringAfterLast('.')
        val args = expr.valueArguments
        return when {
            exprTypename in unaryOpNames -> {
                if (args.size != 1 && args.size != 2) return null
                val childExpr = args[0].getArgumentExpression()
                val masked = args.size == 2
                var (childVector, childLinear, isValue) = process(childExpr, collector) ?: return null
                val handle = vectorHandles[shortName] ?: return null
                val linReplace = unaryLinearReplacements[handle] ?: return null
                var linear = linReplace(childLinear)
                var vectorized = """$childVector
                    .lanewise(VectorOperators.$handle""".trimIndent()
                if (masked) {
                    isValue = false
                    val maskExpr = args[1].getArgumentExpression() ?: return null
                    val (maskVector, maskLinear) = processMask(maskExpr, collector) ?: return null
                    linear = "(if($maskLinear) $linear else $childLinear)"
                    vectorized += ", $maskVector)"
                } else {
                    vectorized += ")"
                }
                Triple(vectorized, linear, isValue)
            }

            exprTypename in binaryOpNames -> {
                if (args.size != 2 && args.size != 3) return null
                val masked = args.size == 3
                val leftExpr = args[0].getArgumentExpression() ?: return null
                val rightExpr = args[1].getArgumentExpression() ?: return null
                val handle = vectorHandles[shortName] ?: return null
                val (leftVector, leftLinear, leftValue) = process(leftExpr, collector) ?: return null
                val (rightVector, rightLinear, rightValue) = process(rightExpr, collector) ?: return null
                var isValue = leftValue && rightValue
                var linear = binaryLinearReplacements[handle]?.invoke(leftLinear, rightLinear) ?: return null

                var vectorized = if (rightValue) """$leftVector.
                        lanewise(VectorOperators.$handle, $rightLinear""".trimIndent()
                else """$leftVector
                    .lanewise(VectorOperators.$handle, $rightVector""".trimIndent()

                if (masked) {
                    isValue = false
                    val maskExpr = args[2].getArgumentExpression() ?: return null
                    val (maskVector, maskLinear) = processMask(maskExpr, collector) ?: return null
                    linear = "(if($maskLinear) $linear else $leftLinear)"
                    vectorized += ", $maskVector)"
                } else {
                    vectorized += ")"
                }
                Triple(
                    vectorized, linear, isValue
                )
            }

            exprTypename == valueType -> {
                if (args.size != 1) return null
                val linear = "${args[0].text}"
                val vectorized = "$vecName.broadcast($vecSpecies, $linear)"
                Triple(vectorized, linear, true)
            }

            exprTypename == primitiveSliceType -> {
                if (args.size != 2 && args.size != 1) return null
                val src = args[0].text
                val offset = when (args.size) {
                    2 -> args[1].text
                    else -> "0"
                }
                Triple(
                    "${vecName}.fromArray($vecSpecies, $src, $offset + _vec_internal_idx)", "$src[$offset + _vec_internal_idx]", false
                )
            }

            exprTypename == ifElseType -> {
                if (args.size != 3) return null
                val mask = args[0].getArgumentExpression() ?: return null
                val left = args[1].getArgumentExpression() ?: return null
                val right = args[2].getArgumentExpression() ?: return null
                val (maskVector, maskLinear) = processMask(mask, collector) ?: return null
                val (leftVector, leftLinear, leftValue) = process(left, collector) ?: return null
                val (rightVector, rightLinear, rightValue) = process(right, collector) ?: return null
                val isValue = leftValue && rightValue
                val linear = "(if($maskLinear) $leftLinear else $rightLinear)"
                val vectorized = """
                    $rightVector.blend($leftVector, $maskVector)""".trimIndent()
                Triple(vectorized, linear, isValue)
            }

            else -> null
        }
    }

    fun processMask(expr: KtExpression?, collector: MessageCollector): Pair<String, String>? {
        if (expr == null) return null
        val exprType = context.getType(expr) ?: return null
        val exprTypename = exprType.getKotlinTypeFqName(false)
        val shortName = exprTypename.substringAfterLast('.')
        if (expr !is KtCallExpression) return null
        val args = expr.valueArguments
        return when {
            exprTypename in maskUnaryOpTypes -> {
                if (args.size != 1) return null
                val child = args[0].getArgumentExpression() ?: return null
                val (vecReplacement, linReplacement) = processMask(child, collector) ?: return null
                val handle = maskHandles[shortName] ?: return null
                val linReplacer = maskUnaryReplacement[handle] ?: return null

                Pair(
                    linReplacer(vecReplacement), linReplacer(linReplacement)
                )
            }

            exprTypename in maskBinaryOpTypes -> {
                if (args.size != 2) return null
                val left = args[0].getArgumentExpression() ?: return null
                val (leftVecReplacement, leftLinReplacement) = processMask(left, collector) ?: return null
                val right = args[0].getArgumentExpression() ?: return null
                val (rightVecReplacement, rightLinReplacement) = processMask(right, collector) ?: return null
                val handle = maskHandles[shortName] ?: return null
                val linReplacer = maskBinaryReplacement[handle] ?: return null
                Pair(
                    linReplacer(leftVecReplacement, rightVecReplacement), linReplacer(leftLinReplacement, rightLinReplacement)
                )
            }

            exprTypename in comparatorTypes -> {
                if (args.size != 2) return null
                val leftExpr = args[0].getArgumentExpression() ?: return null
                val rightExpr = args[1].getArgumentExpression() ?: return null
                val handle = maskHandles[shortName] ?: return null
                val (leftVector, leftLinear, leftValue) = process(leftExpr, collector) ?: return null
                val (rightVector, rightLinear, rightValue) = process(rightExpr, collector) ?: return null
                val isValue = leftValue && rightValue
                val linear = comparatorReplacement[handle]?.invoke(leftLinear, rightLinear) ?: return null
                val vectorized = """
                    $leftVector
                    .compare(VectorOperators.$handle, $rightVector)
                """.trimIndent()
                Pair(vectorized, linear)
            }

            else -> null
        }
    }
}
