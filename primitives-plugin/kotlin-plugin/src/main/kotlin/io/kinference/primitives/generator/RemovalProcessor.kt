package io.kinference.primitives.generator

import io.kinference.primitives.types.*
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs
import org.jetbrains.kotlin.psi.psiUtil.prevLeafs
import org.jetbrains.kotlin.resolve.BindingContext
import java.lang.StringBuilder

class RemovalProcessor(private val context: BindingContext, private val builder: StringBuilder) {
    companion object {
        private val WHITESPACE_TO_DELETE: Key<Boolean> = Key.create("WHITESPACE_TO_DELETE")

        private val importsToRemove = setOf(PrimitiveType::class.qualifiedName, PrimitiveArray::class.qualifiedName,)
    }

    fun shouldRemoveImport(directive: KtImportDirective): Boolean {
        val import = directive.importPath?.toString() ?: return false
        return import in importsToRemove
    }

    fun shouldRemoveAnnotation(annotation: KtAnnotationEntry): Boolean {
        return annotation.isPluginAnnotation(context)
    }

    fun shouldRemoveWhiteSpace(space: PsiWhiteSpace): Boolean {
        return space.getUserData(WHITESPACE_TO_DELETE) == true
    }

    fun prepareRemoval(element: PsiElement) {
        val nextElement = element.nextLeafs.firstOrNull()
        if (nextElement is PsiWhiteSpace) {
            if (nextElement.text.isOneNewLine()) {
                nextElement.putUserData(WHITESPACE_TO_DELETE, true)
            }
        }

        val prevElement = element.prevLeafs.firstOrNull()
        if (prevElement is PsiWhiteSpace && !shouldRemoveWhiteSpace(prevElement)) {
            if (prevElement.text.isOneNewLine()) {
                builder.deleteCharAt(builder.length - 1)
            }
        }
    }

    fun reformat(text: String): String {
        val result = StringBuilder()
        val lines = text.lines()
        for (i in lines.indices) {
            val line = lines[i]
            if (line.isBlank() && i > 0 && lines[i - 1].isBlank()) continue
            //mostly for case } ... }
            if (line.isBlank() && i > 0 && i < lines.size - 1 && lines[i - 1].trim() == lines[i + 1].trim()) continue

            result.append(line + "\n")
        }
        return result.toString()
    }

    private fun String.isOneNewLine() = this == "\n"
}
