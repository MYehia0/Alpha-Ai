package com.example.alpha_ai.core.network.internal

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * What a provider's error body says, reduced to the fields used for classification.
 *
 * @param code the numeric `error.code`, when the provider sends one
 * @param codeName a textual `error.code`, such as `model_not_supported`
 * @param errorType OpenRouter's stable `error.metadata.error_type`
 * @param estimatedSeconds the older Hugging Face `estimated_time` for a loading model
 */
internal data class ErrorInfo(
    val code: Int? = null,
    val codeName: String? = null,
    val errorType: String? = null,
    val estimatedSeconds: Long? = null,
)

/** Parses [body] as JSON, or returns `null` when it is absent or not a JSON object. */
internal fun Json.parseObjectOrNull(body: String?): JsonObject? {
    if (body.isNullOrBlank()) return null
    return try {
        parseToJsonElement(body) as? JsonObject
    } catch (_: SerializationException) {
        null
    }
}

/**
 * Reads the `error` member of a response body. Providers send it either as an object
 * (`{"error": {"code": 429, ...}}`) or as a bare string (`{"error": "..."}`).
 * Returns `null` when the body has no `error` member.
 */
internal fun JsonObject.errorInfoOrNull(): ErrorInfo? {
    val error: JsonElement = this["error"] ?: return null
    val estimate = (this["estimated_time"] as? JsonPrimitive)?.secondsOrNull()
    if (error !is JsonObject) return ErrorInfo(estimatedSeconds = estimate)

    val code = error["code"] as? JsonPrimitive
    val metadata = error["metadata"] as? JsonObject
    return ErrorInfo(
        code = code?.takeUnless { it.isString }?.intOrNull ?: code?.contentOrNull?.toIntOrNull(),
        codeName = code?.takeIf { it.isString }?.contentOrNull?.takeIf { it.toIntOrNull() == null },
        errorType = (metadata?.get("error_type") as? JsonPrimitive)?.contentOrNull,
        estimatedSeconds = estimate,
    )
}

private fun JsonPrimitive.secondsOrNull(): Long? =
    longOrNull ?: contentOrNull?.toDoubleOrNull()?.let { kotlin.math.ceil(it).toLong() }
