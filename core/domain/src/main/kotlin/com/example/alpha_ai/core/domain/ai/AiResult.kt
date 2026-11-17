package com.example.alpha_ai.core.domain.ai

/**
 * The outcome of a one-shot AI request: a value or a classified failure.
 *
 * Cancellation is not a value. When the caller's coroutine is cancelled the call throws
 * `CancellationException` and nothing is delivered.
 */
sealed interface AiResult<out T> {
    data class Success<out T>(val value: T) : AiResult<T>

    data class Failure(val failure: AiFailure) : AiResult<Nothing>
}
