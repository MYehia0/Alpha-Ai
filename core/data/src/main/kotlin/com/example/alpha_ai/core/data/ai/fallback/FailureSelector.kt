package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.network.ProviderFailure

/**
 * What happened to one model choice during a request.
 *
 * @param waitSeconds how long until the choice may work again, when that is known
 */
internal data class AttemptOutcome(val failure: ProviderFailure, val waitSeconds: Long? = null)

/**
 * Picks the one failure to report when every choice failed or was skipped: the category most
 * useful to the user, in the order busy, timed out, unavailable, invalid response, unknown.
 * "Busy" wins because waiting and retrying is something the user can actually do.
 */
internal object FailureSelector {

    fun select(outcomes: List<AttemptOutcome>): AiFailure {
        if (outcomes.isEmpty()) return AiFailure.ServiceUnavailable

        val failures = outcomes.map { it.failure.toAiFailure() }
        return when {
            failures.any { it is AiFailure.ServiceBusy } -> AiFailure.ServiceBusy(
                retryAfterSeconds = outcomes
                    .filter { it.failure.toAiFailure() is AiFailure.ServiceBusy }
                    .mapNotNull { it.waitSeconds ?: it.failure.knownWaitSeconds() }
                    .minOrNull(),
            )
            AiFailure.TimedOut in failures -> AiFailure.TimedOut
            AiFailure.ServiceUnavailable in failures -> AiFailure.ServiceUnavailable
            AiFailure.InvalidResponse in failures -> AiFailure.InvalidResponse
            AiFailure.NoConnection in failures -> AiFailure.NoConnection
            else -> AiFailure.Unknown
        }
    }
}

/** The user-facing category for one provider failure (data-model.md, section 5). */
internal fun ProviderFailure.toAiFailure(): AiFailure = when (this) {
    ProviderFailure.NoConnection -> AiFailure.NoConnection
    ProviderFailure.Timeout -> AiFailure.TimedOut
    is ProviderFailure.RateLimited -> AiFailure.ServiceBusy(retryAfterSeconds)
    is ProviderFailure.QuotaExhausted -> AiFailure.ServiceBusy()
    is ProviderFailure.Loading -> AiFailure.ServiceBusy(estimateSeconds)
    ProviderFailure.Overloaded -> AiFailure.ServiceBusy()
    ProviderFailure.ModelNotFound,
    ProviderFailure.AccessRejected,
    ProviderFailure.NoCredits,
    ProviderFailure.ServerError,
    ProviderFailure.RequestRejected,
    -> AiFailure.ServiceUnavailable
    ProviderFailure.ContentBlocked, ProviderFailure.MalformedResponse -> AiFailure.InvalidResponse
    ProviderFailure.Unknown -> AiFailure.Unknown
}

private fun ProviderFailure.knownWaitSeconds(): Long? = when (this) {
    is ProviderFailure.RateLimited -> retryAfterSeconds
    is ProviderFailure.Loading -> estimateSeconds
    else -> null
}
