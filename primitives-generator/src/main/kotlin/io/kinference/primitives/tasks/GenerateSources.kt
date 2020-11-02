package io.kinference.primitives.tasks

import io.kinference.primitives.annotations.*
import io.kinference.primitives.primitives
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.PrimitiveArray
import io.kinference.primitives.types.PrimitiveType
import io.kinference.primitives.utils.analysis.EnvironmentManager
import io.kinference.primitives.utils.analysis.ParseUtil
import io.kinference.primitives.utils.analysis.ResolveUtil
import io.kinference.primitives.utils.analysis.forced
import io.kinference.primitives.utils.myKtSourceSet
import io.kinference.primitives.utils.psi.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens.IDENTIFIER
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

private val WHITESPACE_TO_DELETE: Key<Boolean> = Key.create("WHITESPACE_TO_DELETE")

@CacheableTask
@ExperimentalUnsignedTypes
open class GenerateSources : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val myAllSources: Set<File>
        get() = project.myKtSourceSet.toSet()

    @get:OutputDirectory
    val generationPath: File?
        get() = primitives.generationPath

    @TaskAction
    fun act() {
        val classpath = project.configurations.getByName("compileClasspath").files

        val manager = EnvironmentManager.create(classpath)
        val ktFiles = ParseUtil.analyze(myAllSources, manager)
        val context = ResolveUtil.analyze(ktFiles, manager).bindingContext

        val primitiveFiles = ktFiles.filter { it.isAnnotatedWith<GenerateWithPrimitives>(context) }
        val primitiveClasses = primitiveFiles.flatMap { it.collectClasses(context) }

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

        val genDir = generationPath ?: project.file("src/main/kotlin-gen")

        data class PrimitiveContext(
            val type1: Primitive<*, *>? = null,
            val type2: Primitive<*, *>? = null,
            val type3: Primitive<*, *>? = null
        )
        for (file in primitiveFiles) {
            for (primitive in Primitive.all()) {
                val builder = StringBuilder()

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
                                isAnnotation<Type3>(context)
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
                        fun KtAnnotationEntry.getTypes(type: String): List<DataType> {
                            return (this.getValue<List<EnumValue>>(context, type) ?: emptyList())
                                .map { DataType.valueOf(it.enumEntryName.asString()) }
                        }

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
                        } else super.visitNamedFunction(function)
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
                        if (element.elementType == IDENTIFIER) {
                            if (element.parent in primitiveClasses) {
                                builder.append(replacements[(element.parent as KtClass).qualifiedName]!!.invoke(currentPrimitive))
                                return
                            }
                        }

                        builder.append(element.text)
                    }
                })

                with(File(genDir,"${file.packageFqName.asString().replace('.', '/')}/${file.name.replace("Primitive", primitive.typeName)}")) {
                    parentFile.mkdirs()
                    writeText(builder.toString())
                }
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
}
