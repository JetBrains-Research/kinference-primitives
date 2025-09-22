package io.kinference.primitives.generator.errors

import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal fun MessageCollector.report(severity: CompilerMessageSeverity, element: KtElement, message: String){
    report(severity, message, element.getLocation())
}

@OptIn(ExperimentalContracts::class)
internal fun MessageCollector.require(severity: CompilerMessageSeverity, element: KtElement, condition: Boolean, message: () -> String) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        report(severity, element, message())
    }
}

fun KtElement.getLocation(): CompilerMessageSourceLocation? {
    val lineToColumn = if (this !is KtFile) StringUtil.offsetToLineColumn(containingKtFile.text, textOffset) else null
    return CompilerMessageLocation.create(containingKtFile.virtualFilePath, lineToColumn?.line ?: 1, lineToColumn?.line ?: 1, null)
}

