package com.example.alpha_ai.core.network

/** One step of a streamed answer. A stream ends with exactly one [Done] or [Error]. */
sealed interface ChatStreamEvent {
    data class Delta(val text: String) : ChatStreamEvent

    data class Done(val truncated: Boolean) : ChatStreamEvent

    data class Error(val failure: ProviderFailure) : ChatStreamEvent
}
