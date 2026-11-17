package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.config.ModelChains
import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.network.ConnectivityChecker
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Runs one request against a capability's chain of models: the first choice, then each
 * fallback, until one returns a usable result, nothing is left, or time runs out.
 *
 * - With no connection it fails at once and tries nothing.
 * - Choices that failed recently are skipped ([ModelHealthTracker]).
 * - Each attempt and the whole request are bounded in time.
 * - No model is tried twice, and nothing is retried: free requests are scarce.
 * - Cancelling the caller stops the current attempt and starts no other.
 *
 * The flow is drawn in specs/002-ai-provider-layer/data-model.md, section 7.
 */
internal class FallbackRunner @Inject constructor(
    private val chains: ModelChains,
    private val health: ModelHealthTracker,
    private val connectivity: ConnectivityChecker,
    private val clock: AiClock,
) {

    /**
     * @param attempt calls one model and returns its cleaned, checked result. Output that
     * fails its check must be returned as [ProviderFailure.MalformedResponse].
     */
    suspend fun run(
        capability: Capability,
        attempt: suspend (ModelChoice) -> ProviderResult<String>,
    ): AiResult<String> {
        val startedAt = clock.nowMillis()
        val limits = TimeLimits.of(capability)

        val result = if (connectivity.isOnline()) {
            tryChain(capability, limits, startedAt, attempt)
        } else {
            AiResult.Failure(AiFailure.NoConnection)
        }

        AiDiagnostics.request(
            capability,
            failure = (result as? AiResult.Failure)?.failure,
            totalMillis = clock.nowMillis() - startedAt,
        )
        return result
    }

    private suspend fun tryChain(
        capability: Capability,
        limits: TimeLimits,
        startedAt: Long,
        attempt: suspend (ModelChoice) -> ProviderResult<String>,
    ): AiResult<String> {
        val outcomes = mutableListOf<AttemptOutcome>()
        var attemptNumber = 0

        for (choice in chains.chainFor(capability)) {
            val remaining = limits.totalMillis - (clock.nowMillis() - startedAt)
            if (remaining < MIN_ATTEMPT_MILLIS) {
                outcomes += AttemptOutcome(ProviderFailure.Timeout)
                break
            }

            val skip = health.skipReason(choice)
            if (skip != null) {
                outcomes += AttemptOutcome(skip.cause, skip.waitSeconds)
                continue
            }

            attemptNumber++
            val attemptStartedAt = clock.nowMillis()
            // A timeout of this attempt returns null here. Cancellation of the caller is a
            // different exception and passes straight through.
            val result = withTimeoutOrNull(minOf(limits.attemptMillis, remaining)) { attempt(choice) }
                ?: ProviderResult.Failure(ProviderFailure.Timeout)
            AiDiagnostics.attempt(
                capability,
                choice,
                attemptNumber,
                failure = (result as? ProviderResult.Failure)?.failure,
                elapsedMillis = clock.nowMillis() - attemptStartedAt,
            )

            when (result) {
                is ProviderResult.Success -> {
                    health.recordSuccess(choice)
                    return AiResult.Success(result.value)
                }

                is ProviderResult.Failure -> {
                    health.recordFailure(choice, result.failure)
                    // Another model cannot fix a missing connection.
                    if (result.failure == ProviderFailure.NoConnection) {
                        return AiResult.Failure(AiFailure.NoConnection)
                    }
                    outcomes += AttemptOutcome(result.failure)
                }
            }
        }
        return AiResult.Failure(FailureSelector.select(outcomes))
    }

    internal companion object {
        /** No new attempt starts with less time than this left. */
        const val MIN_ATTEMPT_MILLIS = 3_000L
    }
}

/**
 * Time bounds for a request. Two full attempts fit inside the total, so a slow first choice
 * cannot use up the whole budget.
 */
internal data class TimeLimits(val attemptMillis: Long, val totalMillis: Long) {
    companion object {
        val TEXT = TimeLimits(attemptMillis = 14_000, totalMillis = 30_000)
        val MEDIA = TimeLimits(attemptMillis = 28_000, totalMillis = 60_000)

        fun of(capability: Capability): TimeLimits = when (capability) {
            Capability.GRAMMAR, Capability.TRANSLATION, Capability.SUMMARIZATION -> TEXT
            Capability.SPEECH, Capability.IMAGE_TEXT -> MEDIA
        }
    }
}
