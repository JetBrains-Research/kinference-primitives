package io.kinference.primitives.utils

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

internal object JSON {
    val json = Json {
        allowStructuredMapKeys = true
    }

    inline fun <reified T : Any> string(serializer: SerializationStrategy<T>, value: T): String {
        return json.encodeToString(serializer, value)
    }

    inline fun <reified T> parse(serializer: DeserializationStrategy<T>, value: String): T {
        return json.decodeFromString(serializer, value)
    }
}
