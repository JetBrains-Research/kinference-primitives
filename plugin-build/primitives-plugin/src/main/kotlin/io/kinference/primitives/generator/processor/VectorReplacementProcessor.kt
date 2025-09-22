package io.kinference.primitives.generator.processor

import io.kinference.primitives.generator.Primitive
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.generator.fqTypename
import io.kinference.primitives.generator.initializer
import io.kinference.primitives.types.DataType
import io.kinference.primitives.vector.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import io.kinference.primitives.generator.errors.*


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
            "SQRT" to { x: String -> "sqrt($x)" },
            "CBRT" to { x: String -> "cbrt($x)" },
        )

        val binaryLinearReplacements = mapOf(
            "ADD" to { x: String, y: String -> "($x + $y)" },
            "SUB" to { x: String, y: String -> "($x - $y)" },
            "MUL" to { x: String, y: String -> "($x * $y)" },
            "DIV" to { x: String, y: String -> "($x / $y)" },
            "MAX" to { x: String, y: String -> "maxOf($x, $y)" },
            "MIN" to { x: String, y: String -> "minOf($x, $y)" },
            "POW" to { x: String, y: String -> "($x).pow($y)" },
        )

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
            "Sqrt" to "SQRT",
            "Cbrt" to "CBRT",
        )

        val supportedTypes = mapOf(
            "EXP" to setOf(DataType.FLOAT, DataType.DOUBLE),
            "LOG" to setOf(DataType.FLOAT, DataType.DOUBLE),
            "SQRT" to setOf(DataType.FLOAT, DataType.DOUBLE),
            "CBRT" to setOf(DataType.FLOAT, DataType.DOUBLE),
        ).withDefault { DataType.ALL.resolve() }

        val maskHandles = mapOf(
            "And" to "AND",
            "Or" to "OR",
            "Xor" to "XOR",
            "Not" to "NOT",
            "Eq" to "EQ",
            "Neq" to "NEQ",
            "LT" to "LT",
            "LE" to "LE",
            "GT" to "GT",
            "GE" to "GE"
        )

        val maskUnaryReplacement = mapOf(
            "Not" to ({ x: String -> "$x.not()" }),
        )

        val maskBinaryReplacement = mapOf(
            "And" to ({ x: String, y: String -> "($x.and($y)" }),
            "Or" to ({ x: String, y: String -> "$x.or($y)" }),
            "Xor" to ({ x: String, y: String -> "$x.xor($y)" }),
        )

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
    )

    var scalarDeclarations: String = ""
    var vecDeclarations: String = ""
    var linDeclarations: String = ""
    var localVariables: Set<String> = emptySet()
    var scalarVariables: Set<String> = emptySet()

    private fun processSimpleName(expr: KtExpression?): Triple<String, String, Boolean>? {
        if (expr !is KtSimpleNameExpression) return null
        val varName = expr.text

        val actualBody = expr.initializer(context)
        if (varName !in localVariables) {
            val success = addVariable(actualBody, varName)
            if (!success) return null
        }
        return Triple("${varName}_vec", "${varName}_lin", varName in scalarVariables)
    }

    private fun addVariable(expr: KtExpression?, varName: String): Boolean {
        val (vecReplacement, linReplacement, scalar) = process(expr) ?: return false
        localVariables = localVariables + varName
        if (scalar) {
            scalarVariables = scalarVariables + varName
            scalarDeclarations += "val ${varName}_vec = $vecReplacement\n"
            scalarDeclarations += "val ${varName}_lin = $linReplacement\n"
        } else {
            vecDeclarations += "val ${varName}_vec = $vecReplacement\n"
            linDeclarations += "val ${varName}_lin = $linReplacement\n"
        }
        return true
    }

    fun process(expr: KtExpression?): Triple<String, String, Boolean>? {
        if (expr !is KtCallExpression) {
            return processSimpleName(expr)
        }

        val exprTypename = expr.fqTypename(context) ?: return null
        val shortName = exprTypename.substringAfterLast('.')
        val args = expr.valueArguments

        return when (exprTypename) {
            in unaryOpNames -> {
                val handle = vectorHandles[shortName] ?: return null
                if (!supportedTypes.getValue(handle).contains(primitive.dataType)) {
                    collector.report(
                        CompilerMessageSeverity.STRONG_WARNING,
                        "$handle operation is not supported for ${primitive.dataType} type",
                        expr.getLocation()
                    )
                    return null
                }
                processUnaryOperation(handle, args)
            }

            in binaryOpNames -> {
                val handle = vectorHandles[shortName] ?: return null
                if (!supportedTypes.getValue(handle).contains(primitive.dataType)) {
                    collector.report(
                        CompilerMessageSeverity.STRONG_WARNING,
                        "$handle operation is not supported for ${primitive.dataType} type",
                        expr.getLocation()
                    )
                    return null
                }
                processBinaryOperation(handle, args)
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
                val vectorized = "${vecName}.fromArray($vecSpecies, $src, $offset + _vec_internal_idx)"
                val linear = "$src[$offset+_vec_internal_idx]"
                Triple(vectorized, linear, false)
            }

            ifElseType -> processIfElse(args)
            else -> null
        }
    }

    private fun processUnaryOperation(handle: String, args: List<KtValueArgument>): Triple<String, String, Boolean>? {
        if (args.size != 1 && args.size != 2) return null
        val childExpr = args[0].getArgumentExpression()
        val masked = args.size == 2
        var (childVector, childLinear, isScalar) = process(childExpr) ?: return null
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
        return Triple(vectorized, linear, isScalar)
    }

    private fun processBinaryOperation(handle: String, args: List<KtValueArgument>): Triple<String, String, Boolean>? {
        if (args.size != 2 && args.size != 3) return null
        val masked = args.size == 3
        val leftExpr = args[0].getArgumentExpression()
        val rightExpr = args[1].getArgumentExpression()
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
        return Triple(vectorized, linear, isScalar)
    }

    private fun processIfElse(args: List<KtValueArgument>): Triple<String, String, Boolean>? {
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
        return Triple(vectorized, linear, isScalar)
    }

    private fun processMask(expr: KtExpression?): Pair<String, String>? {
        if (expr !is KtCallExpression) {
            val (vecReplacement, linReplacement, _) = processSimpleName(expr) ?: return null
            return Pair(vecReplacement, linReplacement)
        }
        val exprTypename = expr.fqTypename(context) ?: return null
        val handle = maskHandles[exprTypename.substringAfterLast('.')] ?: return null
        val args = expr.valueArguments
        return when (exprTypename) {
            in maskUnaryOpTypes -> {
                if (args.size != 1) return null
                val child = args[0].getArgumentExpression() ?: return null
                val (vecReplacement, linReplacement) = processMask(child) ?: return null
                val linReplacer = maskUnaryReplacement[handle] ?: return null

                Pair(
                    linReplacer(vecReplacement), linReplacer(linReplacement)
                )
            }

            in maskBinaryOpTypes -> {
                if (args.size != 2) return null
                val left = args[0].getArgumentExpression() ?: return null
                val (leftVecReplacement, leftLinReplacement) = processMask(left) ?: return null
                val right = args[0].getArgumentExpression() ?: return null
                val (rightVecReplacement, rightLinReplacement) = processMask(right) ?: return null
                val linReplacer = maskBinaryReplacement[handle] ?: return null
                Pair(
                    linReplacer(leftVecReplacement, rightVecReplacement), linReplacer(leftLinReplacement, rightLinReplacement)
                )
            }

            in comparatorTypes -> {
                if (args.size != 2) return null
                val leftExpr = args[0].getArgumentExpression() ?: return null
                val rightExpr = args[1].getArgumentExpression() ?: return null
                val (leftVector, leftLinear, leftScalar) = process(leftExpr) ?: return null
                val (rightVector, rightLinear, rightScalar) = process(rightExpr) ?: return null
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
