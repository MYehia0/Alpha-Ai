package com.example.alpha_ai.core.domain.ai

/**
 * Why an AI request could not be completed, in terms a screen can explain to the user.
 *
 * A failure never carries a status code, a provider or model name, or a raw provider message.
 * [canRetry] says whether offering "Try again" makes sense.
 */
sealed interface AiFailure {
    val canRetry: Boolean

    /** The device has no usable internet connection. */
    data object NoConnection : AiFailure {
        override val canRetry: Boolean = true
    }

    /** No model answered within the time allowed. */
    data object TimedOut : AiFailure {
        override val canRetry: Boolean = true
    }

    /**
     * The models are rate limited, out of free allowance, loading, or overloaded. Retrying
     * later can help; [retryAfterSeconds] is the shortest known wait, when one is known.
     */
    data class ServiceBusy(val retryAfterSeconds: Long? = null) : AiFailure {
        override val canRetry: Boolean = true
    }

    /** The model is gone, access was rejected, or the provider failed. Retrying will not help. */
    data object ServiceUnavailable : AiFailure {
        override val canRetry: Boolean = false
    }

    /** The input cannot be accepted. No model was called. */
    data class InvalidInput(val reason: InvalidInputReason) : AiFailure {
        override val canRetry: Boolean = false
    }

    /** The models answered, but not with a usable result. */
    data object InvalidResponse : AiFailure {
        override val canRetry: Boolean = true
    }

    /** Anything that fits no other category. */
    data object Unknown : AiFailure {
        override val canRetry: Boolean = true
    }
}
