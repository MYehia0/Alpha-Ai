package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ProviderFailure
import javax.inject.Inject
import javax.inject.Singleton

/** The current time in milliseconds. Injected so tests can control it. */
internal fun interface AiClock {
    fun nowMillis(): Long
}

/**
 * Why a model choice is being skipped right now.
 *
 * @param cause the failure that started the cooldown
 * @param waitSeconds how long until it may be tried again; `null` means until the app restarts
 */
internal data class Skip(val cause: ProviderFailure, val waitSeconds: Long?)

/**
 * Remembers which models and providers failed recently, so the next request goes straight to
 * a choice that is likely to work instead of waiting for a failure that is already known.
 *
 * State lives in memory only: a wrong cooldown cannot outlive the process.
 * The cooldown table is in specs/002-ai-provider-layer/research.md, section 9.
 */
@Singleton
internal class ModelHealthTracker @Inject constructor(
    private val clock: AiClock,
) {
    private class Cooldown(val untilMillis: Long, val cause: ProviderFailure)

    private val lock = Any()
    private val models = HashMap<Pair<AiProvider, String>, Cooldown>()
    private val providers = HashMap<AiProvider, Cooldown>()
    private val consecutiveInvalid = HashMap<Pair<AiProvider, String>, Int>()

    /** The reason [choice] must be skipped now, or `null` when it may be tried. */
    fun skipReason(choice: ModelChoice): Skip? = synchronized(lock) {
        val now = clock.nowMillis()
        val active = listOfNotNull(providers[choice.provider], models[choice.key])
            .filter { it.untilMillis > now }
            .maxByOrNull { it.untilMillis }
            ?: return null
        val wait = if (active.untilMillis == UNTIL_RESTART) {
            null
        } else {
            (active.untilMillis - now + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
        }
        Skip(active.cause, wait)
    }

    fun recordSuccess(choice: ModelChoice): Unit = synchronized(lock) {
        models.remove(choice.key)
        consecutiveInvalid.remove(choice.key)
    }

    fun recordFailure(choice: ModelChoice, failure: ProviderFailure): Unit = synchronized(lock) {
        val now = clock.nowMillis()
        fun model(millis: Long) {
            models[choice.key] = Cooldown(now + millis, failure)
        }
        fun provider(untilMillis: Long) {
            providers[choice.provider] = Cooldown(untilMillis, failure)
        }

        if (failure !is ProviderFailure.MalformedResponse) consecutiveInvalid.remove(choice.key)

        when (failure) {
            is ProviderFailure.RateLimited ->
                model(failure.retryAfterSeconds?.times(MILLIS_PER_SECOND) ?: RATE_LIMIT_MILLIS)

            is ProviderFailure.QuotaExhausted ->
                provider(failure.resetAtEpochMillis?.takeIf { it > now } ?: (now + QUOTA_MILLIS))

            is ProviderFailure.Loading ->
                model(failure.estimateSeconds?.times(MILLIS_PER_SECOND) ?: SHORT_MILLIS)

            ProviderFailure.Overloaded, ProviderFailure.Timeout, ProviderFailure.ServerError ->
                model(SHORT_MILLIS)

            ProviderFailure.ModelNotFound -> models[choice.key] = Cooldown(UNTIL_RESTART, failure)

            ProviderFailure.AccessRejected, ProviderFailure.NoCredits -> provider(UNTIL_RESTART)

            ProviderFailure.MalformedResponse -> {
                val count = (consecutiveInvalid[choice.key] ?: 0) + 1
                if (count >= INVALID_LIMIT) {
                    consecutiveInvalid.remove(choice.key)
                    model(INVALID_MILLIS)
                } else {
                    consecutiveInvalid[choice.key] = count
                }
            }

            ProviderFailure.NoConnection,
            ProviderFailure.RequestRejected,
            ProviderFailure.ContentBlocked,
            ProviderFailure.Unknown,
            -> Unit
        }
    }

    private val ModelChoice.key: Pair<AiProvider, String> get() = provider to modelId

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val RATE_LIMIT_MILLIS = 60_000L
        const val QUOTA_MILLIS = 60 * 60_000L
        const val SHORT_MILLIS = 30_000L
        const val INVALID_MILLIS = 5 * 60_000L
        const val INVALID_LIMIT = 3
        const val UNTIL_RESTART = Long.MAX_VALUE
    }
}
