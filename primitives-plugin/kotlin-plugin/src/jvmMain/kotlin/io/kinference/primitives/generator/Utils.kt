package io.kinference.primitives.generator

import io.kinference.primitives.annotations.*
import io.kinference.primitives.generator.errors.require
import io.kinference.primitives.handler.Primitive
import io.kinference.primitives.types.DataType
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import kotlin.reflect.KProperty


internal fun DataType.toPrimitive(): Set<Primitive<*, *>> = this.resolve().map { Primitive.of(it) }.toSet()

internal fun KtAnnotationEntry.getTypes(context: BindingContext, property: KProperty<Array<out DataType>>): List<DataType> {
    return getEnumValueArray(context, property.name, DataType::valueOf)
}


internal fun KtClass.getExcludes(context: BindingContext) = (this as KtAnnotated).getExcludes(context)
internal fun KtNamedFunction.getExcludes(context: BindingContext) = (this as KtAnnotated).getExcludes(context)
private fun KtAnnotated.getExcludes(context: BindingContext): List<DataType> {
    return getAnnotationOrNull<FilterPrimitives>(context)?.getTypes(context, FilterPrimitives::exclude) ?: emptyList()
}


internal fun KtClass.getIncludes(context: BindingContext) = (this as KtAnnotated).getIncludes(context)
internal fun KtNamedFunction.getIncludes(context: BindingContext) = (this as KtAnnotated).getIncludes(context)
private fun KtAnnotated.getIncludes(context: BindingContext): List<DataType>? {
    return getAnnotationOrNull<SpecifyPrimitives>(context)?.getTypes(context, SpecifyPrimitives::include)
}


internal fun KtAnnotationEntry.isPluginAnnotation(context: BindingContext): Boolean {
    return isAnnotation<GeneratePrimitives>(context) ||
        isAnnotation<GenerateNameFromPrimitives>(context) ||
        isAnnotation<BindPrimitives>(context) ||
        isAnnotation<BindPrimitives.Type1>(context) ||
        isAnnotation<BindPrimitives.Type2>(context) ||
        isAnnotation<BindPrimitives.Type3>(context) ||
        isAnnotation<FilterPrimitives>(context) ||
        isAnnotation<SpecifyPrimitives>(context)
}

internal fun DeclarationDescriptor.isNamedFunction() = findPsi() is KtNamedFunction
internal fun DeclarationDescriptor.isKtClassOrObject() = findPsi() is KtClassOrObject
internal fun DeclarationDescriptor.isCompanion() = findPsi() is KtObjectDeclaration && containingDeclaration?.findPsi() is KtClass
internal fun DeclarationDescriptor.isConstructor() = findPsi() is KtConstructor<*> && containingDeclaration?.findPsi() is KtClass

internal fun KtNamedDeclaration.specialize(primitive: Primitive<*, *>, collector: MessageCollector): String {
    val name = name!!
    collector.require(CompilerMessageSeverity.WARNING, this, "Primitive" in name) {
        "Named declaration does not contain \"Primitive\" substring in name, but its name should be specialized. Most likely this problem would lead to redeclaration compile error."
    }
    return name.specialize(primitive)
}

internal fun String.specialize(primitive: Primitive<*, *>) = replace("Primitive", primitive.typeName)

