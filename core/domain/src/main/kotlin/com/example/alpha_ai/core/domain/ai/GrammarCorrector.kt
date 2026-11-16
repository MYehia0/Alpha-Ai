package com.example.alpha_ai.core.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Corrects the grammar and spelling of Arabic or English text, returning only the corrected
 * text in the language of the input. When the language is `null` it is detected.
 *
 * The `suspend` function returns exactly one [AiResult] and never throws, except
 * `CancellationException` when the caller is cancelled. The streaming flow is cold, ends with
 * exactly one [TextStreamEvent.Completed] or [TextStreamEvent.Failed], and never throws
 * except on cancellation. Cancelling the calling coroutine stops the request: nothing is
 * delivered afterwards and no further model is called. Both are safe to call from any
 * dispatcher.
 */
interface GrammarCorrector {
    suspend fun correct(text: String, language: Language? = null): AiResult<String>

    fun correctStreaming(text: String, language: Language? = null): Flow<TextStreamEvent>
}
