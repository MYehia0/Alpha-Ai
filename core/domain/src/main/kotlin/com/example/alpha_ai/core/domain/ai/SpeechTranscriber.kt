package com.example.alpha_ai.core.domain.ai

/**
 * Turns recorded Arabic or English speech into text. A clip with no speech is a success with
 * an empty string.
 *
 * The function returns exactly one [AiResult] and never throws, except
 * `CancellationException` when the caller is cancelled. Cancelling the calling coroutine
 * stops the request: nothing is delivered afterwards and no further model is called. It is
 * safe to call from any dispatcher.
 */
interface SpeechTranscriber {
    suspend fun transcribe(audio: AudioClip, language: Language? = null): AiResult<String>
}
