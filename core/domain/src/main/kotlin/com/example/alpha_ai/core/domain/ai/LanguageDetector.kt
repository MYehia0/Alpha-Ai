package com.example.alpha_ai.core.domain.ai

/**
 * Identifies whether text is Arabic or English. Makes no network call.
 *
 * The function returns exactly one [AiResult] and never throws, except
 * `CancellationException` when the caller is cancelled. Cancelling the calling coroutine
 * stops the request: nothing is delivered afterwards and no further model is called. It is
 * safe to call from any dispatcher.
 */
interface LanguageDetector {
    suspend fun detect(text: String): AiResult<DetectedLanguage>
}
