package com.example.alpha_ai.core.network

/** The outcome of one call to one model: a value or a classified [ProviderFailure]. */
sealed interface ProviderResult<out T> {
    data class Success<out T>(val value: T) : ProviderResult<T>

    data class Failure(val failure: ProviderFailure) : ProviderResult<Nothing>
}
