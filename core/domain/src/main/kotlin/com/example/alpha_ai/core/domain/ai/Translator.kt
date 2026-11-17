package com.example.alpha_ai.core.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Translates text between Arabic and English, returning only the translation.
 *
 * The `suspend` function returns exactly one [AiResult] and never throws, except
 * `CancellationException` when the caller is cancelled. The streaming flow is cold, ends with
 * exactly one [TextStreamEvent.Completed] or [TextStreamEvent.Failed], and never throws
 * except on cancellation. Cancelling the calling coroutine stops the request: nothing is
 * delivered afterwards and no further model is called. Both are safe to call from any
 * dispatcher.
 */
interface Translator {
    suspend fun translate(text: String, from: Language, to: Language): AiResult<String>

    fun translateStreaming(text: String, from: Language, to: Language): Flow<TextStreamEvent>
}
