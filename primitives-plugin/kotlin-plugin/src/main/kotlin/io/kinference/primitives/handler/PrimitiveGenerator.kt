package io.kinference.primitives.handler

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.*
import io.kinference.primitives.utils.crossProduct
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

class PrimitiveGenerator(private val classes: List<KtClass>) {
    companion object {
        private val WHITESPACE_TO_DELETE: Key<Boolean> = Key.create("WHITESPACE_TO_DELETE")

        private val defaultReplacements: Map<String, (Primitive<*, *>) -> String> = mapOf(
            (DataType::class.qualifiedName!! + ".${DataType.UNKNOWN.name}") to { it.dataType.name },

            (PrimitiveType::class.qualifiedName!! + ".toPrimitive") to { "to${it.typeName}" },
            (PrimitiveType::class.java.`package`.name + ".toPrimitive") to { "to${it.typeName}" },

            (PrimitiveType::class.qualifiedName!!) to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".<init>") to { it.typeName },
            (PrimitiveType::class.qualifiedName!! + ".Companion") to { it.typeName },

            (PrimitiveArray::class.qualifiedName!!) to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".<init>") to { it.arrayTypeName },
            (PrimitiveArray::class.qualifiedName!! + ".Companion") to { it.arrayTypeName }
        )
    }

    private data class PrimitiveContext(val type1: Primitive<*, *>? = null, val type2: Primitive<*, *>? = null, val type3: Primitive<*, *>? = null)

    private val replacements = HashMap<String, (Primitive<*, *>) -> String>().apply {
        putAll(defaultReplacements)

        for (klass in classes) {
            put(klass.qualifiedName) { klass.name!!.replace("Primitive", it.typeName) }
            put(klass.qualifiedName + ".<init>") { klass.name!!.replace("Primitive", it.typeName) }
            put(klass.qualifiedName + ".Companion") { klass.name!!.replace("Primitive", it.typeName) }
        }
    }

    fun generate(input: KtFile, context: BindingContext, outputDir: File): Set<File> {
        val outputs = HashSet<File>()

        for (primitive in Primitive.all()) {
            val builder = StringBuilder()
            var isNotEmptyFile = false

            input.accept(object : KtDefaultVisitor() {
                private var currentPrimitive = primitive

                private fun withPrimitive(primitive: Primitive<*, *>?, body: () -> Unit) {
                    require(primitive != null) { "Type not bound" }

                    val tmp = currentPrimitive
                    currentPrimitive = primitive
                    body()
                    currentPrimitive = tmp
                }

                private var primitiveContext = PrimitiveContext()
                private fun withContext(context: PrimitiveContext, body: () -> Unit) {
                    val tmp = primitiveContext
                    primitiveContext = context
                    body()
                    primitiveContext = tmp
                }

                private fun KtAnnotationEntry.isPluginAnnotation(): Boolean {
                    return isAnnotation<GenerateWithPrimitives>(context) ||
                        isAnnotation<PrimitiveClass>(context) ||
                        isAnnotation<PrimitiveBinding>(context) ||
                        isAnnotation<Type1>(context) ||
                        isAnnotation<Type2>(context) ||
                        isAnnotation<Type3>(context) ||
                        isAnnotation<Exclude>(context)
                }

                private fun KtAnnotationEntry.getTypes(type: String): List<DataType> {
                    return (this.getValue<List<EnumValue>>(context, type) ?: emptyList()).map { DataType.valueOf(it.enumEntryName.asString()) }
                }

                override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                    if (annotationEntry.isPluginAnnotation()) {
                        if (!annotationEntry.isAnnotation<PrimitiveBinding>(context)) {
                            (annotationEntry.nextSibling ?: annotationEntry.parent.nextSibling)?.putUserData(WHITESPACE_TO_DELETE, true)
                        }

                        return
                    }

                    super.visitAnnotationEntry(annotationEntry)
                }

                override fun visitWhiteSpace(space: PsiWhiteSpace) {
                    if (space.getUserData(WHITESPACE_TO_DELETE) == true) return

                    super.visitWhiteSpace(space)
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val reference = context[BindingContext.REFERENCE_TARGET, expression]
                    if (reference != null) {
                        val type = reference.forced().fqNameSafe.asString()
                        if (expression.text != "this" && type in replacements) {
                            builder.append(replacements[type]!!.invoke(currentPrimitive))
                            return
                        }
                    }

                    super.visitSimpleNameExpression(expression)
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    val excludes = function.annotationEntries.lastOrNull { it.isAnnotation<Exclude>(context) }?.getTypes("types") ?: emptyList()
                    if (function.isAnnotatedWith<PrimitiveBinding>(context)) {
                        for (annotation in function.annotationEntries.filter { it.isAnnotation<PrimitiveBinding>(context) }) {
                            val types1 = annotation.getTypes("type1")
                            val types2 = annotation.getTypes("type2")
                            val types3 = annotation.getTypes("type3")

                            val combinations = crossProduct(types1, types2, types3)

                            for (combination in combinations) {
                                var index = 0
                                val type1 = if (types1.isEmpty()) null else combination[index++]
                                val type2 = if (types2.isEmpty()) null else combination[index++]
                                val type3 = if (types3.isEmpty()) null else combination[index]

                                withContext(PrimitiveContext(type1?.toPrimitive(), type2?.toPrimitive(), type3?.toPrimitive())) {
                                    super.visitNamedFunction(function)
                                }
                                builder.append('\n')
                            }
                        }
                    } else if (primitive.dataType !in excludes) {
                        isNotEmptyFile = true
                        super.visitNamedFunction(function)
                    }
                }

                override fun visitTypeReference(typeReference: KtTypeReference) {
                    when {
                        typeReference.isAnnotatedWith<Type1>(context) -> withPrimitive(primitiveContext.type1) {
                            super.visitTypeReference(typeReference)
                        }
                        typeReference.isAnnotatedWith<Type2>(context) -> withPrimitive(primitiveContext.type2) {
                            super.visitTypeReference(typeReference)
                        }
                        typeReference.isAnnotatedWith<Type3>(context) -> withPrimitive(primitiveContext.type3) {
                            super.visitTypeReference(typeReference)
                        }
                        else -> super.visitTypeReference(typeReference)
                    }
                }

                override fun visitClass(klass: KtClass) {
                    val excludes = klass.annotationEntries.lastOrNull { it.isAnnotation<Exclude>(context) }?.getTypes("types") ?: emptyList()

                    if (currentPrimitive.dataType !in excludes) {
                        isNotEmptyFile = true
                        super.visitClass(klass)
                    }
                }


                override fun visitAnnotatedExpression(expression: KtAnnotatedExpression) {
                    when {
                        expression.isAnnotatedWith<Type1>(context) -> withPrimitive(primitiveContext.type1) {
                            super.visitAnnotatedExpression(expression)
                        }
                        expression.isAnnotatedWith<Type2>(context) -> withPrimitive(primitiveContext.type2) {
                            super.visitAnnotatedExpression(expression)
                        }
                        expression.isAnnotatedWith<Type3>(context) -> withPrimitive(primitiveContext.type3) {
                            super.visitAnnotatedExpression(expression)
                        }
                        else -> super.visitAnnotatedExpression(expression)
                    }
                }

                override fun visitLeafElement(element: LeafPsiElement) {
                    if (element.elementType == KtTokens.IDENTIFIER && element.parent in classes) {
                        builder.append(replacements[(element.parent as KtClass).qualifiedName]!!.invoke(currentPrimitive))
                        return
                    }

                    builder.append(element.text)
                }
            })

            if (isNotEmptyFile) {
                with(
                    File(
                        outputDir,
                        "${input.packageFqName.asString().replace('.', '/')}/${
                            input.name.replace("Primitive", primitive.typeName)
                        }"
                    )
                ) {
                    outputs.add(this)
                    parentFile.mkdirs()
                    writeText(builder.toString())
                }

            }
        }
        return outputs
    }
}
