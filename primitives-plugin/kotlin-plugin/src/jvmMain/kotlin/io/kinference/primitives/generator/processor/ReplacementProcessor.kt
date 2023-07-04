package io.kinference.primitives.generator.processor

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.generator.*
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.handler.Primitive
import io.kinference.primitives.types.*
import io.kinference.primitives.utils.psi.forced
import io.kinference.primitives.utils.psi.isAnnotatedWith
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
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
            (target.isNamedFunction() || target.isKtClassOrObject()) && target.isAnnotatedWith<GenerateNameFromPrimitives>() -> {
                expression.text.specialize(primitive)
            }
            (target.isCompanion() || target.isConstructor()) && target.containingDeclaration!!.isAnnotatedWith<GenerateNameFromPrimitives>() -> {
                name.specialize(primitive)
            }
            else -> null
        }
    }

}
