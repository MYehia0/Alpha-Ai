package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ContentPart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// Wire models for the OpenAI-style chat completions API that OpenRouter and Hugging Face share.

@Serializable
internal data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Double,
    val stream: Boolean,
)

/** [content] is a plain string for text, or a list of typed parts when media is attached. */
@Serializable
internal data class ChatMessageDto(
    val role: String,
    val content: JsonElement,
)

@Serializable
internal data class ChatCompletionResponseDto(
    val model: String? = null,
    val choices: List<ChatChoiceDto> = emptyList(),
)

@Serializable
internal data class ChatChoiceDto(
    val message: ChatChoiceMessageDto? = null,
    val delta: ChatDeltaDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class ChatChoiceMessageDto(
    val content: JsonElement? = null,
)

@Serializable
internal data class ChatDeltaDto(
    val content: String? = null,
)

internal const val FINISH_REASON_LENGTH = "length"

internal fun ChatRequest.toDto(stream: Boolean): ChatCompletionRequestDto =
    ChatCompletionRequestDto(
        model = model,
        messages = listOf(
            ChatMessageDto(role = "system", content = JsonPrimitive(systemPrompt)),
            ChatMessageDto(role = "user", content = content.toJsonContent()),
        ),
        maxTokens = maxTokens,
        temperature = temperature,
        stream = stream,
    )

private fun List<ContentPart>.toJsonContent(): JsonElement {
    val only = singleOrNull()
    if (only is ContentPart.Text) return JsonPrimitive(only.text)

    return buildJsonArray {
        for (part in this@toJsonContent) {
            add(
                buildJsonObject {
                    when (part) {
                        is ContentPart.Text -> {
                            put("type", "text")
                            put("text", part.text)
                        }

                        is ContentPart.Image -> {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", "data:${part.mimeType};base64,${part.base64}")
                            }
                        }

                        is ContentPart.Audio -> {
                            put("type", "input_audio")
                            putJsonObject("input_audio") {
                                put("data", part.base64)
                                put("format", part.format)
                            }
                        }
                    }
                },
            )
        }
    }
}
