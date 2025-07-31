package io.kinference.primitives.generator.processor

import io.kinference.primitives.generator.Primitive
import io.kinference.primitives.vector.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.resolve.source.getPsi


internal class VectorReplacementProcessor(
    private val context: BindingContext,
    private val primitive: Primitive<*, *>,
    private val collector: MessageCollector,
    private val file: KtFile,
) {
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
        val unaryOpNames = UnaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val binaryOpNames = BinaryOp::class.sealedSubclasses.map { it.qualifiedName }
        val scalarType = Value::class.qualifiedName
        val primitiveSliceType = PrimitiveSlice::class.qualifiedName
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

    var scalarDeclarations: String = ""
    var vecDeclarations: String = ""
    var linDeclarations: String = ""
    var localVariables: Set<String> = emptySet()

    private fun processDeclaration(expr: KtExpression): Triple<String, String, Boolean>? {
        if (expr !is KtSimpleNameExpression) return null
        val varName = expr.text

        val descriptor = context.get(BindingContext.REFERENCE_TARGET, expr) ?: return null //Triple("NOT_DECL: ${expr.text}", "NOT_DECL", false)
        val declaration = descriptor.toSourceElement.getPsi() ?: return null //Triple("NOT_DECL_PSI: ${expr.text}", "NOT_DECL", false)

        if (declaration !is KtVariableDeclaration) return null
        val actualBody = declaration.initializer ?: return null //Triple("NOT_DECL_BODY: ${expr.text}", "NOT_DECL", false)
        //return Triple("BODY: ${actualBody.text}", "", false)
        val (vecReplacement, linReplacement, scalar) = process(actualBody) ?: return null
        if (varName !in localVariables) {
            localVariables = localVariables + varName
            if (scalar) {
                scalarDeclarations += "val ${varName}_vec = $vecReplacement\n"
                scalarDeclarations += "val ${varName}_lin = $linReplacement\n"
            } else {
                vecDeclarations += "val ${varName}_vec = $vecReplacement\n"
                linDeclarations += "val ${varName}_lin = $linReplacement\n"
            }
        }
        return Triple("${varName}_vec", "${varName}_lin", scalar)
    }


    fun process(expr: KtExpression?): Triple<String, String, Boolean>? {
        if (expr == null) return null
        if (expr !is KtCallExpression) {
            return processDeclaration(expr)
        }
        val exprType = context.getType(expr) ?: return null
        val exprTypename = exprType.getKotlinTypeFqName(false)
        val shortName = exprTypename.substringAfterLast('.')
        val args = expr.valueArguments
        return when (exprTypename) {
            in unaryOpNames -> {
                if (args.size != 1 && args.size != 2) return null
                val childExpr = args[0].getArgumentExpression()
                val masked = args.size == 2
                var (childVector, childLinear, isScalar) = process(childExpr) ?: return null
                val handle = vectorHandles[shortName] ?: return null
                val linReplace = unaryLinearReplacements[handle] ?: return null
                var linear = linReplace(childLinear)
                var vectorized = """$childVector
                        .lanewise(VectorOperators.$handle""".trimIndent()
                if (masked) {
                    isScalar = false
                    val maskExpr = args[1].getArgumentExpression()
                    val (maskVector, maskLinear) = processMask(maskExpr) ?: return null
                    linear = "(if($maskLinear) $linear else $childLinear)"
                    vectorized += ", $maskVector)"
                } else {
                    vectorized += ")"
                }
                Triple(vectorized, linear, isScalar)
            }

            in binaryOpNames -> {
                if (args.size != 2 && args.size != 3) return null
                val masked = args.size == 3
                val leftExpr = args[0].getArgumentExpression()
                val rightExpr = args[1].getArgumentExpression()
                val handle = vectorHandles[shortName] ?: return null
                val (leftVector, leftLinear, leftScalar) = process(leftExpr) ?: return null
                val (rightVector, rightLinear, rightScalar) = process(rightExpr) ?: return null
                var isScalar = leftScalar && rightScalar
                var linear = binaryLinearReplacements[handle]?.invoke(leftLinear, rightLinear) ?: return null

                var vectorized = if (rightScalar) """$leftVector.
                            lanewise(VectorOperators.$handle, $rightLinear""".trimIndent()
                else """$leftVector
                        .lanewise(VectorOperators.$handle, $rightVector""".trimIndent()

                if (masked) {
                    isScalar = false
                    val maskExpr = args[2].getArgumentExpression()
                    val (maskVector, maskLinear) = processMask(maskExpr) ?: return null
                    linear = "(if($maskLinear) $linear else $leftLinear)"
                    vectorized += ", $maskVector)"
                } else {
                    vectorized += ")"
                }
                Triple(
                    vectorized, linear, isScalar
                )
            }

            scalarType -> {
                if (args.size != 1) return null
                val visitor = GenerationVisitor(primitive, context, collector, file)
                args[0].accept(visitor)
                val linear = visitor.text()
                val vectorized = "$vecName.broadcast($vecSpecies, $linear)"
                Triple(vectorized, linear, true)
            }

            primitiveSliceType -> {
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

            ifElseType -> {
                if (args.size != 3) return null
                val mask = args[0].getArgumentExpression() ?: return null
                val left = args[1].getArgumentExpression() ?: return null
                val right = args[2].getArgumentExpression() ?: return null
                val (maskVector, maskLinear) = processMask(mask) ?: return null
                val (leftVector, leftLinear, leftScalar) = process(left) ?: return null
                val (rightVector, rightLinear, rightScalar) = process(right) ?: return null
                val isScalar = leftScalar && rightScalar
                val linear = "(if($maskLinear) $leftLinear else $rightLinear)"
                val vectorized = """
                        $rightVector.blend($leftVector, $maskVector)""".trimIndent()
                Triple(vectorized, linear, isScalar)
            }

            else -> null
        }
    }

    fun processMask(expr: KtExpression?): Pair<String, String>? {
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
                val (vecReplacement, linReplacement) = processMask(child) ?: return null
                val handle = maskHandles[shortName] ?: return null
                val linReplacer = maskUnaryReplacement[handle] ?: return null

                Pair(
                    linReplacer(vecReplacement), linReplacer(linReplacement)
                )
            }

            exprTypename in maskBinaryOpTypes -> {
                if (args.size != 2) return null
                val left = args[0].getArgumentExpression() ?: return null
                val (leftVecReplacement, leftLinReplacement) = processMask(left) ?: return null
                val right = args[0].getArgumentExpression() ?: return null
                val (rightVecReplacement, rightLinReplacement) = processMask(right) ?: return null
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
                val (leftVector, leftLinear, leftScalar) = process(leftExpr) ?: return null
                val (rightVector, rightLinear, rightScalar) = process(rightExpr) ?: return null
                val isScalar = leftScalar && rightScalar
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
