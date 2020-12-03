package io.kinference.primitives.generator

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.handler.Primitive
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.any.PrimitiveArray
import io.kinference.primitives.types.any.PrimitiveType
import io.kinference.primitives.types.number.PrimitiveNumberArray
import io.kinference.primitives.types.number.PrimitiveNumberType
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReplacementProcessor(private val replacementContext: GlobalReplacementContext, private val context: BindingContext) {
    data class GlobalReplacementContext(val classes: Set<KtClass>, val functions: Set<KtNamedFunction>)

    companion object {
        fun prepareGlobalContext(context: BindingContext, files: Set<KtFile>): GlobalReplacementContext {
            val classes = HashSet<KtClass>()
            val functions = HashSet<KtNamedFunction>()
            for (file in files) {

                file.accept(object : KtDefaultVisitor() {
                    override fun visitClass(klass: KtClass) {
                        if (klass.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
                            classes.add(klass)
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        if (function.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
                            functions.add(function)
                        }
                    }

                })
            }

            return GlobalReplacementContext(classes, functions)
        }


        private val defaultReplacements: Map<String, (Primitive<*, *>) -> String> = mapOf(
            (DataType::class.qualifiedName!! + ".${DataType.UNKNOWN.name}") to { it.dataType.name },
            (PrimitiveType::class.java.`package`.name + ".toPrimitive") to { "to${it.typeName}" },

            (PrimitiveType::class.qualifiedName!! + ".toPrimitive") to { "to${it.typeName}" },
            (PrimitiveNumberType::class.qualifiedName!! + ".toPrimitive") to { "to${it.typeName}" },


            (PrimitiveType::class.qualifiedName!!) to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".<init>") to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".Companion") to { it.typeName },
            (PrimitiveNumberType::class.qualifiedName!!) to { it.typeName },
            (PrimitiveNumberType::class.qualifiedName!! + ".<init>") to { it.typeName },
            (PrimitiveNumberType::class.qualifiedName!! + ".Companion") to { it.typeName },


            (PrimitiveArray::class.qualifiedName!!) to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".<init>") to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".Companion") to { it.arrayTypeName },
            (PrimitiveNumberArray::class.qualifiedName!!) to { it.arrayTypeName },
            (PrimitiveNumberArray::class.qualifiedName!! + ".<init>") to { it.arrayTypeName },
            (PrimitiveNumberArray::class.qualifiedName!! + ".Companion") to { it.arrayTypeName },
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
        val reference = context[BindingContext.REFERENCE_TARGET, expression] ?: return null
        val forced = reference.forced()
        if (forced is KtAnnotated && forced.isAnnotatedWith<GenerateNameFromPrimitives>(context)) {
            return expression.text.replace("Primitive", primitive.typeName)
        } else {
            val type = reference.forced().fqNameSafe.asString()
            if (expression.text != "this" && type in replacements) {
                return replacements[type]!!.invoke(primitive)
            }
        }
        return null
    }

    /** Simple name replacements used to replace calls to functions and classes */
    private fun getSimpleNameReplacements(): Map<String, (Primitive<*, *>) -> String> {
        val replacements = HashMap<String, (Primitive<*, *>) -> String>()
        replacements.putAll(defaultReplacements)

        for (klass in replacementContext.classes) {
            replacements[klass.qualifiedName] = { klass.name!!.replace("Primitive", it.typeName) }
            replacements[klass.qualifiedName + ".<init>"] = { klass.name!!.replace("Primitive", it.typeName) }
            replacements[klass.qualifiedName + ".Companion"] = { klass.name!!.replace("Primitive", it.typeName) }
        }

        for (function in replacementContext.functions) {
            replacements[function.qualifiedName] = { function.name!!.replace("Primitive", it.typeName) }
        }

        return replacements
    }
}
