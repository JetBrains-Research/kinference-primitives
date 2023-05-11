# KInference Primitives

KInference Primitives is a library that makes possible generation of primitive versions for generic types.

It supports the Kotlin Multiplatform and is capable of generating common code that would be possible to reuse between JS and JVM targets.

# Setting up

To use KInference Primitives library you will need to set up resolution of a plugin in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven(url = "https://packages.jetbrains.team/maven/p/ki/maven")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.kinference.primitives") {
                useModule("io.kinference.primitives:gradle-plugin-jvm:${requested.version}")
            }
        }
    }
}
```

And then apply it to the project you are working on:

```kotlin
plugins {
    id("io.kinference.primitives") version "0.1.21" apply true
}
```

Also, you will need dependency to annotation library that KInference Primitives is using:

```kotlin

repositories {
    maven(url = "https://packages.jetbrains.team/maven/p/ki/maven")
}

dependencies {
    api("io.kinference.primitives:primitives-annotations:0.1.21")
}
```

# Usage

Here is a very simple example of how to use the KInference Primitives:

```kotlin
@file:GeneratePrimitives(DataType.FLOAT, DataType.INT)

package test

import io.kinference.primitives.annotations.GenerateNameFromPrimitives
import io.kinference.primitives.annotations.GeneratePrimitives
import io.kinference.primitives.types.*

@GenerateNameFromPrimitives
class ClassPrimitiveTest {
    val a = PrimitiveType.MIN_VALUE

    companion object {
        val x: PrimitiveType = PrimitiveType.MAX_VALUE
    }
}
```

This code would generate specializations for Float and Int types via replacement of `Primitive` part in the name of the class and by replacement of all usages
of `PrimitiveType` with corresponding type (Float or Int). Also, note that standard functions, like MAX_VALUE, are available for `PrimitiveType` like it would
be a real `Number`.

