package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.ProviderFailure
import okhttp3.Headers

/**
 * Turns an OpenRouter error into a [ProviderFailure].
 *
 * OpenRouter's `error.metadata.error_type` is the stable field and is read first; the numeric
 * code is the fallback. The code in the body is preferred over the HTTP status because an
 * error that happens after a provider accepted the request arrives with status 200.
 * See specs/002-ai-provider-layer/contracts/provider-api.md.
 */
internal object OpenRouterErrorClassifier : ErrorClassifier {

    override fun classify(status: Int, headers: Headers, error: ErrorInfo?): ProviderFailure {
        val code = error?.code ?: status
        return when (error?.errorType) {
            "rate_limit_exceeded" -> rateLimit(headers)
            "provider_overloaded" -> ProviderFailure.Overloaded
            "provider_unavailable" -> ProviderFailure.ServerError
            "not_found" -> ProviderFailure.ModelNotFound
            "authentication", "permission_denied" -> ProviderFailure.AccessRejected
            "payment_required" -> ProviderFailure.NoCredits
            "content_policy_violation", "refusal" -> ProviderFailure.ContentBlocked
            else -> byCode(code, headers)
        }
    }

    private fun byCode(code: Int, headers: Headers): ProviderFailure = when (code) {
        429 -> rateLimit(headers)
        503 -> ProviderFailure.Overloaded
        404 -> ProviderFailure.ModelNotFound
        401, 403 -> ProviderFailure.AccessRejected
        402 -> ProviderFailure.NoCredits
        408 -> ProviderFailure.Timeout
        in 500..599 -> ProviderFailure.ServerError
        in 400..499 -> ProviderFailure.RequestRejected
        else -> ProviderFailure.Unknown
    }

    /** A 429 that reports no requests remaining means the daily free allowance is spent. */
    private fun rateLimit(headers: Headers): ProviderFailure =
        if (headers["X-RateLimit-Remaining"]?.trim() == "0") {
            ProviderFailure.QuotaExhausted(headers["X-RateLimit-Reset"]?.trim()?.toLongOrNull())
        } else {
            ProviderFailure.RateLimited(headers.retryAfterSeconds())
        }
}

/** Maps a provider's error response to a [ProviderFailure]. */
internal fun interface ErrorClassifier {
    fun classify(status: Int, headers: Headers, error: ErrorInfo?): ProviderFailure
}

/** The `Retry-After` header as whole seconds, when it is a number. */
internal fun Headers.retryAfterSeconds(): Long? =
    this["Retry-After"]?.trim()?.toLongOrNull()?.takeIf { it >= 0 }
