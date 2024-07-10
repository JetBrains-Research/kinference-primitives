package io.kinference.primitives.utils.psi

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtVisitorVoid

internal abstract class KtDefaultVisitor : KtVisitorVoid() {
    protected open fun shouldVisitElement(element: PsiElement) = true

    override fun visitElement(element: PsiElement) {
        if (!shouldVisitElement(element)) return

        if (element is LeafPsiElement) visitLeafElement(element)
        else element.acceptChildren(this)
    }

    open fun visitLeafElement(element: LeafPsiElement) {}
}
