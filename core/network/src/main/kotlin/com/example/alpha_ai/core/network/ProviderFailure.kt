package com.example.alpha_ai.core.network

/**
 * Why one attempt on one model failed. The data layer decides from this whether to try the
 * next model and what the user is finally told.
 */
sealed interface ProviderFailure {
    /** The device is offline, or the provider's host cannot be reached. */
    data object NoConnection : ProviderFailure

    data object Timeout : ProviderFailure

    data class RateLimited(val retryAfterSeconds: Long? = null) : ProviderFailure

    /** The account's allowance for the period is spent. Affects every model on the provider. */
    data class QuotaExhausted(val resetAtEpochMillis: Long? = null) : ProviderFailure

    /** The model is starting up. */
    data class Loading(val estimateSeconds: Long? = null) : ProviderFailure

    data object Overloaded : ProviderFailure

    /** The model does not exist, or no longer exists, on this provider. */
    data object ModelNotFound : ProviderFailure

    /** The key is missing, invalid, or lacks permission. */
    data object AccessRejected : ProviderFailure

    /** The account has no credits for this provider. */
    data object NoCredits : ProviderFailure

    data object ServerError : ProviderFailure

    /** The provider refused the request as invalid or too large for this model. */
    data object RequestRejected : ProviderFailure

    /** A content filter or the model itself declined the content. */
    data object ContentBlocked : ProviderFailure

    /** The reply could not be parsed, had no usable text, or was cut off in transit. */
    data object MalformedResponse : ProviderFailure

    data object Unknown : ProviderFailure
}
