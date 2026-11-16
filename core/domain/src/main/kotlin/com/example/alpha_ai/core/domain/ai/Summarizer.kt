package com.example.alpha_ai.core.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Summarizes Arabic or English text, returning a shorter text in the same language. When the
 * language is `null` it is detected.
 *
 * The `suspend` function returns exactly one [AiResult] and never throws, except
 * `CancellationException` when the caller is cancelled. The streaming flow is cold, ends with
 * exactly one [TextStreamEvent.Completed] or [TextStreamEvent.Failed], and never throws
 * except on cancellation. Cancelling the calling coroutine stops the request: nothing is
 * delivered afterwards and no further model is called. Both are safe to call from any
 * dispatcher.
 */
interface Summarizer {
    suspend fun summarize(text: String, language: Language? = null): AiResult<String>

    fun summarizeStreaming(text: String, language: Language? = null): Flow<TextStreamEvent>
}
