package io.kinference.primitives.handler

import io.kinference.primitives.annotations.*
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.PrimitiveArray
import io.kinference.primitives.types.PrimitiveType
import io.kinference.primitives.utils.psi.*
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.io.File

private val WHITESPACE_TO_DELETE: Key<Boolean> = Key.create("WHITESPACE_TO_DELETE")

class PrimitivesGeneratorAnalysisHandler(private val outputDir: File, private val incrementalDir: File?) : AnalysisHandlerExtension {
    private var generated = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (generated)
            return null

        val incrementalCache = ICCache(incrementalDir)
        val resolveSession = componentProvider.get<ResolveSession>()
        val context = bindingTrace.bindingContext

        files as MutableCollection

        val primitiveFiles = files.filter { resolveSession.getFileAnnotations(it).hasAnnotation(PRIMITIVE_FILE) }
        val analyzedPrimitivesFiles = incrementalCache.analyzeInput(primitiveFiles)

        if (analyzedPrimitivesFiles.isEmpty())
            return null

        componentProvider.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, analyzedPrimitivesFiles)

        val primitiveClasses = analyzedPrimitivesFiles.flatMap { it.collectClasses(context) }

        val replacements = HashMap<String, (Primitive<*, *>) -> String>().apply {
            put(DataType::class.qualifiedName!! + ".${DataType.UNKNOWN.name}") { it.dataType.name }

            put(PrimitiveType::class.qualifiedName!! + ".toPrimitive") { "to${it.typeName}" }
            put(PrimitiveType::class.java.`package`.name + ".toPrimitive") { "to${it.typeName}" }

            put(PrimitiveType::class.qualifiedName!!) { it.typeName }
            put(PrimitiveType::class.qualifiedName!! + ".<init>") { it.typeName }
            put(PrimitiveType::class.qualifiedName!! + ".Companion") { it.typeName }

            put(PrimitiveArray::class.qualifiedName!!) { it.arrayTypeName }
            put(PrimitiveArray::class.qualifiedName!! + ".<init>") { it.arrayTypeName }
            put(PrimitiveArray::class.qualifiedName!! + ".Companion") { it.arrayTypeName }

            for (klass in primitiveClasses) {
                put(klass.qualifiedName) { klass.name!!.replace("Primitive", it.typeName) }
                put(klass.qualifiedName + ".<init>") { klass.name!!.replace("Primitive", it.typeName) }
                put(klass.qualifiedName + ".Companion") { klass.name!!.replace("Primitive", it.typeName) }
            }
        }


        val newFiles = mutableListOf<File>()

        data class PrimitiveContext(
            val type1: Primitive<*, *>? = null,
            val type2: Primitive<*, *>? = null,
            val type3: Primitive<*, *>? = null
        )
        for (file in analyzedPrimitivesFiles) {

            for (primitive in Primitive.all()) {
                val builder = StringBuilder()
                var isNotEmptyFile = false

                file.accept(object : KtDefaultVisitor() {
                    private var currentPrimitive = primitive
                    private fun withPrimitive(primitive: Primitive<*, *>?, body: () -> Unit) {
                        if (primitive == null) throw IllegalStateException("Type not bound")

                        val buffer = currentPrimitive
                        currentPrimitive = primitive
                        body()
                        currentPrimitive = buffer
                    }

                    private var primitiveContext = PrimitiveContext()
                    private fun withContext(context: PrimitiveContext, body: () -> Unit) {
                        val buffer = primitiveContext
                        primitiveContext = context
                        body()
                        primitiveContext = buffer
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
                        return (this.getValue<List<EnumValue>>(context, type) ?: emptyList())
                            .map { DataType.valueOf(it.enumEntryName.asString()) }
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
                        } else if (!excludes.contains(primitive.dataType)) {
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

                        if (!excludes.contains(currentPrimitive.dataType)) {
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
                        if (element.elementType == KtTokens.IDENTIFIER) {
                            if (element.parent in primitiveClasses) {
                                builder.append(replacements[(element.parent as KtClass).qualifiedName]!!.invoke(currentPrimitive))
                                return
                            }
                        }

                        builder.append(element.text)
                    }
                })

                if (isNotEmptyFile)
                    with(File(outputDir,"${file.packageFqName.asString().replace('.', '/')}/${file.name.replace("Primitive", primitive.typeName)}")) {
                        files.removeIf { it.name == this.name }
                        newFiles.add(this)
                        parentFile.mkdirs()
                        writeText(builder.toString())
                    }
            }
        }

        generated = true

        return when {
            bindingTrace.bindingContext.diagnostics.any { it.severity == Severity.ERROR } -> {
                AnalysisResult.compilationError(bindingTrace.bindingContext)
            }
            newFiles.isEmpty() -> null
            else -> {
                AnalysisResult.RetryWithAdditionalRoots(
                    bindingContext = bindingTrace.bindingContext,
                    moduleDescriptor = module,
                    additionalKotlinRoots = newFiles,
                    additionalJavaRoots = emptyList(),
                    addToEnvironment = true
                )
            }
        }
    }

    private fun KtFile.collectClasses(context: BindingContext): Set<KtClass> {
        val classes = HashSet<KtClass>()
        accept(object : KtDefaultVisitor() {
            override fun visitClass(klass: KtClass) {
                if (klass.isAnnotatedWith<PrimitiveClass>(context)) {
                    classes.add(klass)
                }
            }
        })
        return classes
    }

    companion object {
        private val PRIMITIVE_FILE = FqName(GenerateWithPrimitives::class.qualifiedName!!)
    }
}

