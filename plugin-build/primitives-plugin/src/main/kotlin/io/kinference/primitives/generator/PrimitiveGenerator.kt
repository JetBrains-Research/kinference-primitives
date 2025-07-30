package io.kinference.primitives.generator

import io.kinference.primitives.annotations.*
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.generator.processor.RemovalProcessor
import io.kinference.primitives.generator.processor.ReplacementProcessor
import io.kinference.primitives.generator.processor.VectorReplacementProcessor
import io.kinference.primitives.types.DataType
import io.kinference.primitives.utils.crossProduct
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.io.File

internal class PrimitiveGenerator(
    private val file: KtFile,
    private val context: BindingContext,
    private val output: File,
    private val collector: MessageCollector,
    private val vectorize: Boolean = false
) {

    private data class PrimitiveContext(val type1: Primitive<*, *>? = null, val type2: Primitive<*, *>? = null, val type3: Primitive<*, *>? = null)

    private var vecCount = 0;

    fun generate(): Set<File> {
        val results = HashSet<File>()

        val types = file.getAnnotation<GeneratePrimitives>(context).getTypes(context, GeneratePrimitives::types)
        collector.require(CompilerMessageSeverity.WARNING, file, types.isNotEmpty()) {
            "There are no `DataType`s specified in @${GeneratePrimitives::class.simpleName}. It would lead to omitting of file during generation"
        }

        for (primitive in types.flatMap { it.toPrimitive() }.toSet()) {
            val builder = StringBuilder()

            val removalProcessor = RemovalProcessor(context)
            val replacementProcessor = ReplacementProcessor(context, collector, vectorize)

            file.accept(object : KtDefaultVisitor() {
                private var currentPrimitive = primitive

                private fun KtElement.withPrimitive(primitive: Primitive<*, *>?, body: () -> Unit) {
                    collector.require(CompilerMessageSeverity.ERROR, this, primitive != null) {
                        "Primitive was bound with @${BindPrimitives::class.simpleName} sub-annotation," +
                            " but outer expression is not annotated with @${BindPrimitives::class.simpleName}"
                    }

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

                override fun visitImportList(importList: KtImportList) {
                    if (file.isAnnotatedWith<GenerateVector>(context) && primitive.dataType in DataType.VECTORIZABLE.resolve() && vectorize)
                        builder.appendLine("import io.kinference.ndarray.VecUtils.isModuleLoaded")
                        builder.appendLine("import jdk.incubator.vector.*")
                    super.visitImportList(importList)
                }

                override fun visitModifierList(list: KtModifierList) {
                    if (replacementProcessor.shouldChangeVisibilityModifier(list)) {
                        replacementProcessor.prepareReplaceText(list.visibilityModifier(), "public")
                    }

                    super.visitModifierList(list)
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

                    return super.visitImportDirective(importDirective)
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    if (primitive.dataType in function.getExcludes(context)) return
                    if (function.isAnnotatedWith<SpecifyPrimitives>(context) && primitive.dataType !in function.getIncludes(context)!!) return

                    if (function.isAnnotatedWith<BindPrimitives>(context)) {
                        for (annotation in function.annotationEntries.filter { it.isAnnotation<BindPrimitives>(context) }) {
                            val primitives1 = annotation.getTypes(context, BindPrimitives::type1).flatMap { it.toPrimitive() }.toSet()
                            val primitives2 = annotation.getTypes(context, BindPrimitives::type2).flatMap { it.toPrimitive() }.toSet()
                            val primitives3 = annotation.getTypes(context, BindPrimitives::type3).flatMap { it.toPrimitive() }.toSet()


                            collector.require(
                                CompilerMessageSeverity.WARNING, annotation,
                                primitives1.isNotEmpty() || primitives2.isNotEmpty() || primitives3.isNotEmpty()
                            ) {
                                "All arguments of @${BindPrimitives::class.simpleName} are empty. It would lead to omitting of the function during generation."
                            }

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
                    if (klass.isAnnotatedWith<SpecifyPrimitives>(context) && primitive.dataType !in klass.getIncludes(context)!!) return

                    super.visitClass(klass)
                }

                override fun visitTypeReference(typeReference: KtTypeReference) {
                    when {
                        typeReference.isAnnotatedWith<BindPrimitives.Type1>(context) -> typeReference.withPrimitive(primitiveContext.type1) {
                            super.visitTypeReference(typeReference)
                        }

                        typeReference.isAnnotatedWith<BindPrimitives.Type2>(context) -> typeReference.withPrimitive(primitiveContext.type2) {
                            super.visitTypeReference(typeReference)
                        }

                        typeReference.isAnnotatedWith<BindPrimitives.Type3>(context) -> typeReference.withPrimitive(primitiveContext.type3) {
                            super.visitTypeReference(typeReference)
                        }

                        else -> super.visitTypeReference(typeReference)
                    }
                }


                override fun visitAnnotatedExpression(expression: KtAnnotatedExpression) {
                    when {
                        expression.isAnnotatedWith<BindPrimitives.Type1>(context) -> expression.withPrimitive(primitiveContext.type1) {
                            super.visitAnnotatedExpression(expression)
                        }

                        expression.isAnnotatedWith<BindPrimitives.Type2>(context) -> expression.withPrimitive(primitiveContext.type2) {
                            super.visitAnnotatedExpression(expression)
                        }

                        expression.isAnnotatedWith<BindPrimitives.Type3>(context) -> expression.withPrimitive(primitiveContext.type3) {
                            super.visitAnnotatedExpression(expression)
                        }

                        else -> super.visitAnnotatedExpression(expression)
                    }
                }

                override fun visitLeafElement(element: LeafPsiElement) {
                    if (replacementProcessor.haveReplaceText(element)) {
                        builder.append(replacementProcessor.getReplacement(element))
                        return
                    }

                    if (element.elementType != KtTokens.IDENTIFIER) {
                        builder.append(element.text)
                        return
                    }

                    when (val parent = element.parent) {
                        is KtClassOrObject -> builder.append(replacementProcessor.getReplacement(parent, currentPrimitive) ?: element.text)
                        is KtNamedFunction -> builder.append(replacementProcessor.getReplacement(parent, currentPrimitive) ?: element.text)
                        else -> builder.append(element.text)
                    }
                }

                override fun visitDeclaration(dcl: KtDeclaration) {
                    if (file.isAnnotatedWith<GenerateVector>(context) && dcl is KtProperty) {
                        val init = dcl.initializer
                        if (init != null) {
                            val type = context.getType(init)
                            if (type != null) {
                                val supertypes = type.supertypes().map { it.getKotlinTypeFqName(false) }.toSet()
                                if (VectorReplacementProcessor.opNodeTypename in supertypes) return
                            }
                        }
                    }
                    super.visitDeclaration(dcl)
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val replacement = replacementProcessor.getReplacement(expression, currentPrimitive)
                    builder.append(replacement ?: expression.text)
                }

                override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                    if (!file.isAnnotatedWith<GenerateVector>(context)) {
                        super.visitDotQualifiedExpression(expression)
                        return
                    }
                    val replacement = replacementProcessor.getReplacement(expression, currentPrimitive, vecCount)
                    if (replacement == null) {
                        super.visitDotQualifiedExpression(expression); return
                    } else {
                        vecCount += 1
                        builder.append(replacement)
                    }
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
