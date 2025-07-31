package io.kinference.primitives.generator

import io.kinference.primitives.annotations.*
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.generator.processor.GenerationVisitor
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
            val visitor = GenerationVisitor(primitive, context, collector, file)
            file.accept(visitor)
            val text = visitor.text()

            if (text.isNotBlank()) {
                val file = File(
                    output,
                    "${file.packageFqName.asString().replace('.', '/')}/${file.name.replace("Primitive", primitive.typeName)}"
                )
                results.add(file)
                file.parentFile.mkdirs()
                file.writeText(visitor.removalProcessor.reformat(text))
            }
        }

        return results
    }
}
