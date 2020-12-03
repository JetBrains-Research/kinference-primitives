package io.kinference.primitives.generator

import io.kinference.primitives.annotations.BindPrimitives
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.handler.Primitive
import io.kinference.primitives.utils.crossProduct
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

class PrimitiveGenerator(private val file: KtFile, private val context: BindingContext, private val output: File,
                         private val collector: MessageCollector, private val replacementContext: ReplacementProcessor.GlobalReplacementContext) {

    private data class PrimitiveContext(val type1: Primitive<*, *>? = null, val type2: Primitive<*, *>? = null, val type3: Primitive<*, *>? = null)

    fun generate(): Set<File> {
        val results = HashSet<File>()

        val types = file.getAnnotation<GeneratePrimitives>(context).getTypes(context, GeneratePrimitives::types)
        for (primitive in types.flatMap { it.toPrimitive() }.toSet()) {
            val builder = StringBuilder()

            val removalProcessor = RemovalProcessor(context, builder)
            val replacementProcessor = ReplacementProcessor(replacementContext, context)

            file.accept(object : KtDefaultVisitor() {
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

                override fun visitWhiteSpace(space: PsiWhiteSpace) {
                    if (removalProcessor.shouldRemoveWhiteSpace(space)) return

                    super.visitWhiteSpace(space)
                }

                override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                    if (removalProcessor.shouldRemoveAnnotation(annotationEntry)) {
                        removalProcessor.prepareRemoval(annotationEntry)
                        return
                    }

                    builder.append(annotationEntry.text)
                }

                override fun visitImportDirective(importDirective: KtImportDirective) {
                    if (removalProcessor.shouldRemoveImport(importDirective)) {
                        removalProcessor.prepareRemoval(importDirective)
                        return
                    }

                    builder.append(importDirective.text)
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    if (primitive.dataType in function.getExcludes(context)) return

                    if (function.isAnnotatedWith<BindPrimitives>(context)) {
                        for (annotation in function.annotationEntries.filter { it.isAnnotation<BindPrimitives>(context) }) {
                            val primitives1 = annotation.getTypes(context, BindPrimitives::type1).flatMap { it.toPrimitive() }.toSet()
                            val primitives2 = annotation.getTypes(context, BindPrimitives::type2).flatMap { it.toPrimitive() }.toSet()
                            val primitives3 = annotation.getTypes(context, BindPrimitives::type3).flatMap { it.toPrimitive() }.toSet()

                            val combinations = crossProduct(primitives1, primitives2, primitives3)

                            for (combination in combinations) {
                                var index = 0
                                val primitive1 = if (primitives1.isEmpty()) null else combination[index++]
                                val primitive2 = if (primitives2.isEmpty()) null else combination[index++]
                                val primitive3 = if (primitives3.isEmpty()) null else combination[index]

                                withContext(PrimitiveContext(primitive1, primitive2, primitive3)) {
                                    super.visitNamedFunction(function)
                                }
                                builder.append('\n')
                            }
                        }
                    } else {
                        super.visitNamedFunction(function)
                    }
                }

                override fun visitClass(klass: KtClass) {
                    if (primitive.dataType in klass.getExcludes(context)) return

                    super.visitClass(klass)
                }

                override fun visitTypeReference(typeReference: KtTypeReference) {
                    when {
                        typeReference.isAnnotatedWith<BindPrimitives.Type1>(context) -> withPrimitive(primitiveContext.type1) {
                            super.visitTypeReference(typeReference)
                        }
                        typeReference.isAnnotatedWith<BindPrimitives.Type2>(context) -> withPrimitive(primitiveContext.type2) {
                            super.visitTypeReference(typeReference)
                        }
                        typeReference.isAnnotatedWith<BindPrimitives.Type3>(context) -> withPrimitive(primitiveContext.type3) {
                            super.visitTypeReference(typeReference)
                        }
                        else -> super.visitTypeReference(typeReference)
                    }
                }


                override fun visitAnnotatedExpression(expression: KtAnnotatedExpression) {
                    when {
                        expression.isAnnotatedWith<BindPrimitives.Type1>(context) -> withPrimitive(primitiveContext.type1) {
                            super.visitAnnotatedExpression(expression)
                        }
                        expression.isAnnotatedWith<BindPrimitives.Type2>(context) -> withPrimitive(primitiveContext.type2) {
                            super.visitAnnotatedExpression(expression)
                        }
                        expression.isAnnotatedWith<BindPrimitives.Type3>(context) -> withPrimitive(primitiveContext.type3) {
                            super.visitAnnotatedExpression(expression)
                        }
                        else -> super.visitAnnotatedExpression(expression)
                    }
                }

                override fun visitLeafElement(element: LeafPsiElement) {
                    if (element.elementType != KtTokens.IDENTIFIER) {
                        builder.append(element.text)
                        return
                    }

                    when (val parent = element.parent) {
                        is KtClass -> builder.append(replacementProcessor.getReplacement(parent, currentPrimitive) ?: element.text)
                        is KtNamedFunction -> builder.append(replacementProcessor.getReplacement(parent, currentPrimitive) ?: element.text)
                        else -> builder.append(element.text)
                    }
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val replacement = replacementProcessor.getReplacement(expression, currentPrimitive)
                    builder.append(replacement ?: expression.text)
                }
            })

            if (builder.isNotBlank()) {
                val file = File(
                    output,
                    "${file.packageFqName.asString().replace('.', '/')}/${file.name.replace("Primitive", primitive.typeName)}"
                )
                results.add(file)
                file.parentFile.mkdirs()
                file.writeText(removalProcessor.reformat(builder.toString()))
            }
        }

        return results
    }
}
