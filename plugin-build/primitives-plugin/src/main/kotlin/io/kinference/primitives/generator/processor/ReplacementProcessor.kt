package io.kinference.primitives.generator.processor

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.annotations.GenerateVector
import io.kinference.primitives.annotations.MakePublic
import io.kinference.primitives.generator.*
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.types.*
import io.kinference.primitives.vector.*
import io.kinference.primitives.utils.psi.forced
import io.kinference.primitives.utils.psi.isAnnotatedWith
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.supertypes

internal class ReplacementProcessor(
    private val context: BindingContext,
    private val collector: MessageCollector,
    private val file: KtFile,
    private val vectorize: Boolean = true
) {
    companion object {
        internal fun toType(primitive: Primitive<*, *>): String {
            return when (primitive.dataType) {
                DataType.BYTE -> "toInt().toByte"
                DataType.SHORT -> "toInt().toShort"
                else -> "to${primitive.typeName}"
            }
        }

        private val REPLACE_TEXT: Key<String> = Key.create("REPLACE_TEXT_IN_MODIFIER")

        private val defaultReplacements: Map<String, (Primitive<*, *>) -> String> = mapOf(
            (DataType::class.qualifiedName!! + ".Companion.${DataType.Companion::CurrentPrimitive.name}") to { it.dataType.name },
            (PrimitiveType::class.java.`package`.name + ".toPrimitive") to this::toType,

            (PrimitiveType::class.qualifiedName!! + ".toPrimitive") to this::toType,

            (PrimitiveType::class.qualifiedName!!) to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".<init>") to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".Companion") to { it.typeName },

            (PrimitiveArray::class.qualifiedName!!) to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".<init>") to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".Companion") to { it.arrayTypeName }
        )

    }


    fun getReplacement(klass: KtClassOrObject, primitive: Primitive<*, *>): String? {
        if (!klass.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
            collector.require(CompilerMessageSeverity.WARNING, klass, !klass.isTopLevel()) {
                "Class is not annotated with ${GenerateNameFromPrimitives::class.simpleName}, so its name would not be specialized. It may lead to redeclaration compile error."
            }
            return null
        }

        return klass.specialize(primitive, collector)

    }

    fun getReplacement(function: KtNamedFunction, primitive: Primitive<*, *>): String? {
        if (function.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
            return function.specialize(primitive, collector)
        }
        collector.require(CompilerMessageSeverity.WARNING, function, !function.isTopLevel || function.isExtensionDeclaration()) {
            "Function is not annotated with ${GenerateNameFromPrimitives::class.simpleName}, so its name would not be specialized. It may lead to redeclaration compile error."
        }
        return null
    }

    fun getReplacement(expression: KtSimpleNameExpression, primitive: Primitive<*, *>): String? {
        val name = expression.text?.takeIf { it.contains("Primitive") } ?: return null
        val target = context[BindingContext.REFERENCE_TARGET, expression]?.forced() ?: return null
        val type = target.fqNameSafe.asString()

        return when {
            type in defaultReplacements -> {
                defaultReplacements[type]!!.invoke(primitive)
            }

            (target.isKtClassOrObject() && target.containingDeclaration!!.isAnnotatedWith<GenerateNameFromPrimitives>()) ||
                (target.isNamedFunction() || target.isKtClassOrObject()) && target.isAnnotatedWith<GenerateNameFromPrimitives>() -> {
                expression.text.specialize(primitive)
            }

            (target.isCompanion() || target.isConstructor()) && target.containingDeclaration!!.isAnnotatedWith<GenerateNameFromPrimitives>() -> {
                name.specialize(primitive)
            }

            else -> null
        }
    }

    fun getReplacement(expr: KtDotQualifiedExpression, primitive: Primitive<*, *>, idx: Int): String? {
        val receiver = expr.receiverExpression
        val selector = expr.selectorExpression ?: return null
        if (selector !is KtCallExpression) return null

        val args = selector.valueArguments
        val callName = selector.calleeExpression?.text ?: return null

        if (!isVectorClass(receiver, context)) return null

        val vecProcessor = VectorReplacementProcessor(context, primitive, collector, file)
        val (vecReplacement, linReplacement, isScalar) = vecProcessor.process(receiver) ?: return null

        val toPrimitive = "${toType(primitive)}()"
        val vecLen = "_vecLen_$idx"
        val vecEnd = "_vecEnd_$idx"
        val vecIdx = "_vec_internal_idx"
        val vecEnabled = "isModuleLoaded"

        if (callName == "into" && args.size == 3) {
            val dest = args[0].text
            val destOffset = args[1].text
            val len = args[2].text

            if (primitive.dataType in DataType.VECTORIZABLE.resolve() && vectorize)
                return """
                if($vecEnabled) {
                    val $vecLen = ${vecProcessor.vecSpecies}.length()
                    val $vecEnd = $len - ($len % $vecLen)
                    ${vecProcessor.scalarDeclarations}
                    for ($vecIdx in 0 until $vecEnd step $vecLen) {
                        ${vecProcessor.vecDeclarations}
                        $vecReplacement.intoArray($dest, $destOffset + _vec_internal_idx)
                    }
                    for($vecIdx in $vecEnd until $len) {
                        ${vecProcessor.linDeclarations}
                        $dest[$destOffset + $vecIdx] = $linReplacement.$toPrimitive
                    }
                }else{
                    for($vecIdx in 0 until $len) {
                        ${vecProcessor.linDeclarations}
                        $dest[$destOffset + $vecIdx] = $linReplacement.$toPrimitive
                    } 
               }
                """.trimIndent()
            else
                return """
                for($vecIdx in 0 until $len) {
                    ${vecProcessor.linDeclarations}
                    $dest[$destOffset + $vecIdx] = $linReplacement.$toPrimitive
               }""".trimIndent()
        } else if (callName == "reduce" && args.size == 2) {
            val handle = args[0].text
            val len = args[1].text

            if (VectorReplacementProcessor.isAssoc[handle] != true) return ""
            val neutral = vecProcessor.neutralElement[handle] ?: return ""

            val linearOp = VectorReplacementProcessor.binaryLinearReplacements[handle] ?: return ""
            val linAccumulate = linearOp("ret", linReplacement)

            if (primitive.dataType in DataType.VECTORIZABLE.resolve() && vectorize) {
                return """{
                    if($vecEnabled) {
                        val $vecLen = ${vecProcessor.vecSpecies}.length()
                        val $vecEnd = $len - ($len % $vecLen)
                        var accumulator = ${vecProcessor.vecName}.broadcast(${vecProcessor.vecSpecies}, $neutral)
                        ${vecProcessor.scalarDeclarations}
                        for ($vecIdx in 0 until $vecEnd step $vecLen) {
                            ${vecProcessor.vecDeclarations}
                            accumulator = accumulator.lanewise(VectorOperators.$handle, $vecReplacement)
                        }
                        var ret = accumulator.reduceLanes(VectorOperators.$handle)
                        for($vecIdx in $vecEnd until $len) {
                            ${vecProcessor.linDeclarations}
                            ret = $linAccumulate.$toPrimitive
                        }
                        ret.$toPrimitive
                    }else{
                        var ret = $neutral
                        for($vecIdx in 0 until $len) {
                            ${vecProcessor.linDeclarations}
                            ret = $linAccumulate.$toPrimitive
                        }
                    ret.$toPrimitive}
                }.invoke()
                """.trimIndent()
            } else {
                return """{
                    var ret = $neutral
                    for($vecIdx in 0 until $len) {
                        ${vecProcessor.linDeclarations}
                        ret = $linAccumulate.$toPrimitive
                    }
                    ret.$toPrimitive
                }.invoke()
                """.trimIndent()
            }

        } else return ""
    }


    fun shouldChangeVisibilityModifier(list: KtModifierList): Boolean {
        val owner = list.owner
        val visibilityModifier = list.visibilityModifier()

        return visibilityModifier != null && owner is KtAnnotated && owner.isAnnotatedWith<MakePublic>(context)
    }

    fun prepareReplaceText(psi: PsiElement?, newText: String) {
        psi?.putUserData(REPLACE_TEXT, newText)
    }

    fun haveReplaceText(psi: LeafPsiElement): Boolean {
        return psi.getUserData(REPLACE_TEXT) != null
    }

    fun getReplacement(psi: LeafPsiElement): String {
        return psi.getUserData(REPLACE_TEXT)!!
    }
}
