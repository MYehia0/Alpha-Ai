package com.example.alpha_ai.core.network

/**
 * A provider-neutral chat request: one instruction and the user's content.
 *
 * @param model the provider's identifier for the model
 * @param systemPrompt the instruction; never contains user content
 * @param content the user's content, sent as a separate message
 */
data class ChatRequest(
    val model: String,
    val systemPrompt: String,
    val content: List<ContentPart>,
    val maxTokens: Int,
    val temperature: Double,
)

/** One piece of user content. */
sealed interface ContentPart {
    data class Text(val text: String) : ContentPart

    /** @param base64 the image bytes, base64-encoded without a `data:` prefix */
    data class Image(val base64: String, val mimeType: String) : ContentPart

    /**
     * @param base64 the audio bytes, base64-encoded without a `data:` prefix
     * @param format the short format name the provider expects, such as `wav` or `mp3`
     */
    data class Audio(val base64: String, val format: String) : ContentPart
}
