package com.example.alpha_ai.core.domain.ai

/**
 * Reads the printed Arabic or English text in an image. An image with no text is a success
 * with an empty string.
 *
 * The function returns exactly one [AiResult] and never throws, except
 * `CancellationException` when the caller is cancelled. Cancelling the calling coroutine
 * stops the request: nothing is delivered afterwards and no further model is called. It is
 * safe to call from any dispatcher.
 */
interface ImageTextExtractor {
    suspend fun extractText(image: ImageInput): AiResult<String>
}
