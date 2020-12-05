package io.kinference.primitives.generator

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.handler.Primitive
import io.kinference.primitives.types.*
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReplacementProcessor(private val context: BindingContext) {
    companion object {
        private val defaultReplacements: Map<String, (Primitive<*, *>) -> String> = mapOf(
            (DataType::class.qualifiedName!! + ".${DataType.UNKNOWN.name}") to { it.dataType.name },
            (PrimitiveType::class.java.`package`.name + ".toPrimitive") to { "to${it.typeName}" },

            (PrimitiveType::class.qualifiedName!! + ".toPrimitive") to { "to${it.typeName}" },


            (PrimitiveType::class.qualifiedName!!) to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".<init>") to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".Companion") to { it.typeName },


            (PrimitiveArray::class.qualifiedName!!) to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".<init>") to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".Companion") to { it.arrayTypeName }
        )
    }


    private val replacements = getSimpleNameReplacements()

    fun getReplacement(klass: KtClass, primitive: Primitive<*, *>): String? {
        if (klass.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
            return klass.name!!.replace("Primitive", primitive.typeName)
        }
        return null
    }

    fun getReplacement(function: KtNamedFunction, primitive: Primitive<*, *>): String? {
        if (function.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
            return function.name!!.replace("Primitive", primitive.typeName)
        }
        return null
    }

    fun getReplacement(expression: KtSimpleNameExpression, primitive: Primitive<*, *>): String? {
        val name = expression.text?.takeIf { it.contains("Primitive") } ?: return null

        val reference = context[BindingContext.REFERENCE_TARGET, expression]?.forced() ?: return null
        val type = reference.fqNameSafe.asString()
        if (expression.text != "this" && type in replacements) {
            return replacements[type]!!.invoke(primitive)
        } else {
            val original = reference.original
            val originalPsi = original.findPsi()
            if ((originalPsi is KtNamedFunction || originalPsi is KtClass) && (originalPsi as KtAnnotated).isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
                return name.replace("Primitive", primitive.typeName)
            } else if (originalPsi is KtObjectDeclaration || originalPsi is KtConstructor<*>) {
                val containingPsi = original.containingDeclaration?.findPsi() ?: return null
                if (containingPsi is KtClass && containingPsi.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
                    return name.replace("Primitive", primitive.typeName)
                }
            }
            return null
        }
    }

    /** Simple name replacements used to replace calls to functions and classes */
    private fun getSimpleNameReplacements(): Map<String, (Primitive<*, *>) -> String> {
        val replacements = HashMap<String, (Primitive<*, *>) -> String>()
        replacements.putAll(defaultReplacements)

        return replacements
    }
}
