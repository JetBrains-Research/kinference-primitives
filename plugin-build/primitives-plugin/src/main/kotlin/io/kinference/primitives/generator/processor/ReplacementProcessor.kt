package io.kinference.primitives.generator.processor

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.annotations.MakePublic
import io.kinference.primitives.generator.*
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.types.*
import io.kinference.primitives.utils.psi.forced
import io.kinference.primitives.utils.psi.isAnnotatedWith
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

internal class ReplacementProcessor(private val context: BindingContext, private val collector: MessageCollector) {
    companion object {
        private fun toType(primitive: Primitive<*, *>): String {
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
        if (klass.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
            return klass.specialize(primitive, collector)
        }
        collector.require(CompilerMessageSeverity.WARNING, klass, !klass.isTopLevel()) {
            "Class is not annotated with ${GenerateNameFromPrimitives::class.simpleName}, so its name would not be specialized. It may lead to redeclaration compile error."
        }
        return null
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
