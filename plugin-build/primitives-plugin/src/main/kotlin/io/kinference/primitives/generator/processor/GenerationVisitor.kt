package io.kinference.primitives.generator.processor

import io.kinference.primitives.annotations.BindPrimitives
import io.kinference.primitives.annotations.GenerateVector
import io.kinference.primitives.annotations.SpecifyPrimitives
import io.kinference.primitives.generator.Primitive
import io.kinference.primitives.generator.PrimitiveGenerator.PrimitiveContext
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.generator.getExcludes
import io.kinference.primitives.generator.getIncludes
import io.kinference.primitives.generator.getTypes
import io.kinference.primitives.generator.isVectorClass
import io.kinference.primitives.generator.toPrimitive
import io.kinference.primitives.types.DataType
import io.kinference.primitives.utils.crossProduct
import io.kinference.primitives.utils.psi.KtDefaultVisitor
import io.kinference.primitives.utils.psi.isAnnotatedWith
import io.kinference.primitives.utils.psi.isAnnotation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.BindingContext

internal class GenerationVisitor(
    private val primitive: Primitive<*, *>,
    private val context: BindingContext,
    private val collector: MessageCollector,
    private val file: KtFile
) :
    KtDefaultVisitor() {
    private data class PrimitiveContext(val type1: Primitive<*, *>? = null, val type2: Primitive<*, *>? = null, val type3: Primitive<*, *>? = null)

    private val vectorize = true
    private val builder = StringBuilder()
    fun text() = builder.toString()
    val removalProcessor = RemovalProcessor(context)
    val replacementProcessor = ReplacementProcessor(context, collector, file, vectorize)
    private var currentPrimitive = primitive
    private var vecCount = 0

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
        if (file.isAnnotatedWith<GenerateVector>(context) && primitive.dataType in DataType.VECTORIZABLE.resolve() && vectorize) {
            builder.appendLine("import io.kinference.ndarray.VecUtils.isModuleLoaded")
            builder.appendLine("import jdk.incubator.vector.*")
        }
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
            if (isVectorClass(init, context)) return
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
}
