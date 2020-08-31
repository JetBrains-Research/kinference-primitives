package io.kinference.primitives.tasks

import io.kinference.primitives.annotations.GenerateWithPrimitives
import io.kinference.primitives.annotations.PrimitiveClass
import io.kinference.primitives.primitives
import io.kinference.primitives.types.DataType
import io.kinference.primitives.types.PrimitiveArray
import io.kinference.primitives.types.PrimitiveType
import io.kinference.primitives.utils.analysis.EnvironmentManager
import io.kinference.primitives.utils.analysis.ParseUtil
import io.kinference.primitives.utils.analysis.ResolveUtil
import io.kinference.primitives.utils.analysis.forced
import io.kinference.primitives.utils.myKtSourceSet
import io.kinference.primitives.utils.psi.KtDefaultVisitor
import io.kinference.primitives.utils.psi.isAnnotatedWith
import io.kinference.primitives.utils.psi.isAnnotation
import io.kinference.primitives.utils.psi.qualifiedName
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens.IDENTIFIER
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

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

        val primitives = mapOf(
                Primitive.create<Byte, ByteArray>(DataType.BYTE) to StringBuilder(),
                Primitive.create<Short, ShortArray>(DataType.SHORT) to StringBuilder(),
                Primitive.create<Int, IntArray>(DataType.INT) to StringBuilder(),
                Primitive.create<Long, LongArray>(DataType.LONG) to StringBuilder(),

                Primitive.create<UByte, UByteArray>(DataType.UBYTE) to StringBuilder(),
                Primitive.create<UShort, UShortArray>(DataType.USHORT) to StringBuilder(),
                Primitive.create<UInt, UIntArray>(DataType.UINT) to StringBuilder(),
                Primitive.create<ULong, ULongArray>(DataType.ULONG) to StringBuilder(),

                Primitive.create<Float, FloatArray>(DataType.FLOAT) to StringBuilder(),
                Primitive.create<Double, DoubleArray>(DataType.DOUBLE) to StringBuilder()
        )

        for (file in ktFiles) {
            if (!file.isAnnotatedWith<GenerateWithPrimitives>(context)) continue

            val classes = file.collectClasses(context)

            for ((primitive, builder) in primitives) {
                val replacements = HashMap<String, String>().apply {
                    put(DataType::class.qualifiedName!! + ".${DataType.UNKNOWN.name}", primitive.dataType.name)

                    put(PrimitiveType::class.qualifiedName!! + ".toPrimitive", "to${primitive.typeName}")
                    put(PrimitiveType::class.java.`package`.name + ".toPrimitive", "to${primitive.typeName}")

                    put(PrimitiveType::class.qualifiedName!!, primitive.typeName!!)
                    put(PrimitiveType::class.qualifiedName!! + ".<init>", primitive.typeName)
                    put(PrimitiveType::class.qualifiedName!! + ".Companion", primitive.typeName)

                    put(PrimitiveArray::class.qualifiedName!!, primitive.arrayTypeName!!)
                    put(PrimitiveArray::class.qualifiedName!! + ".<init>", primitive.arrayTypeName)
                    put(PrimitiveArray::class.qualifiedName!! + ".Companion", primitive.arrayTypeName)

                    for (klass in classes) {
                        val replacement = klass.name!!.replace("Primitive", primitive.typeName)
                        put(klass.qualifiedName, replacement)
                        put(klass.qualifiedName + ".<init>", replacement)
                        put(klass.qualifiedName + ".Companion", replacement)
                    }
                }

                file.accept(object : KtDefaultVisitor() {
                    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                        if (annotationEntry.isAnnotation<GenerateWithPrimitives>(context) ||
                                annotationEntry.isAnnotation<PrimitiveClass>(context)) return

                        super.visitAnnotationEntry(annotationEntry)
                    }

                    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                        val reference = context[BindingContext.REFERENCE_TARGET, expression]
                        if (reference != null) {
                            val type = reference.forced().fqNameSafe.asString()
                            if (expression.text != "this" && type in replacements) {
                                builder.append(replacements[type])
                                return
                            }
                        }

                        super.visitSimpleNameExpression(expression)
                    }

                    override fun visitLeafElement(element: LeafPsiElement) {
                        if (element.elementType == IDENTIFIER) {
                            if (element.parent in classes) {
                                builder.append(replacements[(element.parent as KtClass).qualifiedName])
                                return
                            }
                        }

                        builder.append(element.text)
                    }
                })

                val genDir = generationPath ?: project.file("src/main/kotlin-gen")

                with(File(genDir, "${file.packageFqName.asString().replace('.', '/')}/${file.name.replace("Primitive", primitive.typeName!!)}")) {
                    parentFile.mkdirs()
                    writeText(builder.toString())
                }

                builder.clear()
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