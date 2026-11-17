package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.config.ModelChains
import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.TextStreamEvent
import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.ConnectivityChecker
import com.example.alpha_ai.core.network.ProviderFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * The streaming form of [FallbackRunner]: the same chain, cooldowns, and time limits, with
 * the text delivered as it is generated.
 *
 * - `Delta` for each piece of text as it arrives.
 * - `Restarted` when an attempt fails after sending text and another model takes over; the
 *   screen must discard what it has shown.
 * - Exactly one terminal event: `Completed` with the cleaned and checked full text (which
 *   can differ slightly from the deltas), or `Failed`.
 */
internal class StreamingFallbackRunner @Inject constructor(
    private val chains: ModelChains,
    private val health: ModelHealthTracker,
    private val connectivity: ConnectivityChecker,
    private val clock: AiClock,
) {

    /**
     * @param attempt opens the stream of one model
     * @param check cleans and checks the full text of a finished stream; `null` means unusable
     */
    fun run(
        capability: Capability,
        attempt: (ModelChoice) -> Flow<ChatStreamEvent>,
        check: (raw: String, truncated: Boolean) -> String?,
    ): Flow<TextStreamEvent> = flow {
        val startedAt = clock.nowMillis()
        val terminal = if (connectivity.isOnline()) {
            tryChain(capability, startedAt, attempt, check)
        } else {
            TextStreamEvent.Failed(AiFailure.NoConnection)
        }
        AiDiagnostics.request(
            capability,
            failure = (terminal as? TextStreamEvent.Failed)?.failure,
            totalMillis = clock.nowMillis() - startedAt,
        )
        emit(terminal)
    }

    /** Emits deltas and restarts, and returns the terminal event. */
    private suspend fun FlowCollector<TextStreamEvent>.tryChain(
        capability: Capability,
        startedAt: Long,
        attempt: (ModelChoice) -> Flow<ChatStreamEvent>,
        check: (raw: String, truncated: Boolean) -> String?,
    ): TextStreamEvent {
        val limits = TimeLimits.of(capability)
        val outcomes = mutableListOf<AttemptOutcome>()
        var attemptNumber = 0
        var textShown = false

        for (choice in chains.chainFor(capability)) {
            val remaining = limits.totalMillis - (clock.nowMillis() - startedAt)
            if (remaining < FallbackRunner.MIN_ATTEMPT_MILLIS) {
                outcomes += AttemptOutcome(ProviderFailure.Timeout)
                break
            }
            val skip = health.skipReason(choice)
            if (skip != null) {
                outcomes += AttemptOutcome(skip.cause, skip.waitSeconds)
                continue
            }

            // Another model is taking over from one that had already sent text.
            if (textShown) {
                emit(TextStreamEvent.Restarted)
                textShown = false
            }

            attemptNumber++
            val attemptStartedAt = clock.nowMillis()
            val text = StringBuilder()
            // A timeout of this attempt leaves `end` null. Cancellation of the collector is
            // a different exception and passes straight through.
            val end: ChatStreamEvent? = withTimeoutOrNull(minOf(limits.attemptMillis, remaining)) {
                var last: ChatStreamEvent? = null
                attempt(choice)
                    .takeWhile { event ->
                        last = event
                        event is ChatStreamEvent.Delta
                    }
                    .collect { event ->
                        val piece = (event as ChatStreamEvent.Delta).text
                        text.append(piece)
                        textShown = true
                        emit(TextStreamEvent.Delta(piece))
                    }
                // A stream that stops without saying how it ended was cut off.
                last?.takeUnless { it is ChatStreamEvent.Delta }
                    ?: ChatStreamEvent.Error(ProviderFailure.MalformedResponse)
            }

            val checked = (end as? ChatStreamEvent.Done)?.let { check(text.toString(), it.truncated) }
            val failure: ProviderFailure? = when {
                end == null -> ProviderFailure.Timeout
                end is ChatStreamEvent.Error -> end.failure
                checked == null -> ProviderFailure.MalformedResponse
                else -> null
            }
            AiDiagnostics.attempt(capability, choice, attemptNumber, failure, clock.nowMillis() - attemptStartedAt)

            if (failure == null) {
                health.recordSuccess(choice)
                return TextStreamEvent.Completed(checked.orEmpty())
            }
            health.recordFailure(choice, failure)
            // Another model cannot fix a missing connection.
            if (failure == ProviderFailure.NoConnection) return TextStreamEvent.Failed(AiFailure.NoConnection)
            outcomes += AttemptOutcome(failure)
        }
        return TextStreamEvent.Failed(FailureSelector.select(outcomes))
    }
}
