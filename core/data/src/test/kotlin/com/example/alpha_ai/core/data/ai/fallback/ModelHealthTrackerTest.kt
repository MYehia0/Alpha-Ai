package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ProviderFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelHealthTrackerTest {

    private var now = 1_000_000L
    private val tracker = ModelHealthTracker { now }
    private val a = ModelChoice(AiProvider.OPENROUTER, "a", rank = 10)
    private val b = ModelChoice(AiProvider.OPENROUTER, "b", rank = 20)
    private val other = ModelChoice(AiProvider.HUGGING_FACE, "h", rank = 30)

    private fun advanceSeconds(seconds: Long) {
        now += seconds * 1_000
    }

    @Test
    fun `a healthy model is not skipped`() {
        assertNull(tracker.skipReason(a))
    }

    @Test
    fun `rate limited skips the model for the stated wait`() {
        tracker.recordFailure(a, ProviderFailure.RateLimited(retryAfterSeconds = 20))

        assertEquals(Skip(ProviderFailure.RateLimited(20), waitSeconds = 20), tracker.skipReason(a))
        assertNull(tracker.skipReason(b))
        advanceSeconds(19)
        assertEquals(1L, tracker.skipReason(a)?.waitSeconds)
        advanceSeconds(1)
        assertNull(tracker.skipReason(a))
    }

    @Test
    fun `rate limited with no stated wait skips the model for a minute`() {
        tracker.recordFailure(a, ProviderFailure.RateLimited(retryAfterSeconds = null))

        assertEquals(60L, tracker.skipReason(a)?.waitSeconds)
        advanceSeconds(60)
        assertNull(tracker.skipReason(a))
    }

    @Test
    fun `a spent daily limit skips the whole provider until its reset`() {
        tracker.recordFailure(a, ProviderFailure.QuotaExhausted(resetAtEpochMillis = now + 7_200_000))

        assertEquals(7_200L, tracker.skipReason(a)?.waitSeconds)
        assertEquals(7_200L, tracker.skipReason(b)?.waitSeconds)
        assertNull(tracker.skipReason(other))
        advanceSeconds(7_200)
        assertNull(tracker.skipReason(b))
    }

    @Test
    fun `a spent daily limit with no reset time skips the provider for an hour`() {
        tracker.recordFailure(a, ProviderFailure.QuotaExhausted(resetAtEpochMillis = null))

        assertEquals(3_600L, tracker.skipReason(b)?.waitSeconds)
        advanceSeconds(3_600)
        assertNull(tracker.skipReason(b))
    }

    @Test
    fun `loading skips the model for the estimate or thirty seconds`() {
        tracker.recordFailure(a, ProviderFailure.Loading(estimateSeconds = 45))
        tracker.recordFailure(b, ProviderFailure.Loading(estimateSeconds = null))

        assertEquals(45L, tracker.skipReason(a)?.waitSeconds)
        assertEquals(30L, tracker.skipReason(b)?.waitSeconds)
    }

    @Test
    fun `overloaded timeout and server error skip the model for thirty seconds`() {
        for (failure in listOf(ProviderFailure.Overloaded, ProviderFailure.Timeout, ProviderFailure.ServerError)) {
            val tracker = ModelHealthTracker { now }
            tracker.recordFailure(a, failure)

            assertEquals(Skip(failure, waitSeconds = 30), tracker.skipReason(a))
            assertNull(tracker.skipReason(b))
        }
    }

    @Test
    fun `a model that is not found is skipped until restart`() {
        tracker.recordFailure(a, ProviderFailure.ModelNotFound)
        advanceSeconds(1_000_000)

        assertEquals(Skip(ProviderFailure.ModelNotFound, waitSeconds = null), tracker.skipReason(a))
        assertNull(tracker.skipReason(b))
    }

    @Test
    fun `rejected access and no credits skip the provider until restart`() {
        for (failure in listOf(ProviderFailure.AccessRejected, ProviderFailure.NoCredits)) {
            val tracker = ModelHealthTracker { now }
            tracker.recordFailure(a, failure)
            advanceSeconds(1_000_000)

            assertEquals(Skip(failure, waitSeconds = null), tracker.skipReason(b))
            assertNull(tracker.skipReason(other))
        }
    }

    @Test
    fun `invalid output skips the model for five minutes only after three in a row`() {
        tracker.recordFailure(a, ProviderFailure.MalformedResponse)
        tracker.recordFailure(a, ProviderFailure.MalformedResponse)
        assertNull(tracker.skipReason(a))

        tracker.recordFailure(a, ProviderFailure.MalformedResponse)
        assertEquals(300L, tracker.skipReason(a)?.waitSeconds)
    }

    @Test
    fun `a success between invalid outputs restarts the count`() {
        tracker.recordFailure(a, ProviderFailure.MalformedResponse)
        tracker.recordFailure(a, ProviderFailure.MalformedResponse)
        tracker.recordSuccess(a)
        tracker.recordFailure(a, ProviderFailure.MalformedResponse)

        assertNull(tracker.skipReason(a))
    }

    @Test
    fun `failures that say nothing about the model start no cooldown`() {
        val neutral = listOf(
            ProviderFailure.NoConnection,
            ProviderFailure.RequestRejected,
            ProviderFailure.ContentBlocked,
            ProviderFailure.Unknown,
        )
        neutral.forEach { tracker.recordFailure(a, it) }

        assertNull(tracker.skipReason(a))
    }
}

class FailureSelectorTest {

    private fun select(vararg outcomes: AttemptOutcome) = FailureSelector.select(outcomes.toList())

    @Test
    fun `busy outranks every other category`() {
        assertEquals(
            AiFailure.ServiceBusy(),
            select(
                AttemptOutcome(ProviderFailure.Timeout),
                AttemptOutcome(ProviderFailure.ServerError),
                AttemptOutcome(ProviderFailure.Overloaded),
                AttemptOutcome(ProviderFailure.MalformedResponse),
            ),
        )
    }

    @Test
    fun `busy reports the shortest known wait`() {
        assertEquals(
            AiFailure.ServiceBusy(retryAfterSeconds = 12),
            select(
                AttemptOutcome(ProviderFailure.RateLimited(retryAfterSeconds = 40)),
                AttemptOutcome(ProviderFailure.Loading(estimateSeconds = 90), waitSeconds = 12),
                AttemptOutcome(ProviderFailure.QuotaExhausted(resetAtEpochMillis = null)),
                AttemptOutcome(ProviderFailure.ServerError, waitSeconds = 3),
            ),
        )
    }

    @Test
    fun `timed out outranks unavailable invalid and unknown`() {
        assertEquals(
            AiFailure.TimedOut,
            select(
                AttemptOutcome(ProviderFailure.ModelNotFound),
                AttemptOutcome(ProviderFailure.Timeout),
                AttemptOutcome(ProviderFailure.MalformedResponse),
                AttemptOutcome(ProviderFailure.Unknown),
            ),
        )
    }

    @Test
    fun `unavailable outranks invalid and unknown`() {
        assertEquals(
            AiFailure.ServiceUnavailable,
            select(
                AttemptOutcome(ProviderFailure.ContentBlocked),
                AttemptOutcome(ProviderFailure.NoCredits),
                AttemptOutcome(ProviderFailure.Unknown),
            ),
        )
    }

    @Test
    fun `invalid response outranks unknown`() {
        assertEquals(
            AiFailure.InvalidResponse,
            select(AttemptOutcome(ProviderFailure.Unknown), AttemptOutcome(ProviderFailure.MalformedResponse)),
        )
    }

    @Test
    fun `unknown alone is unknown and nothing tried is unavailable`() {
        assertEquals(AiFailure.Unknown, select(AttemptOutcome(ProviderFailure.Unknown)))
        assertEquals(AiFailure.ServiceUnavailable, select())
    }

    @Test
    fun `every provider failure maps to its category`() {
        val expected = mapOf(
            ProviderFailure.NoConnection to AiFailure.NoConnection,
            ProviderFailure.Timeout to AiFailure.TimedOut,
            ProviderFailure.RateLimited(5) to AiFailure.ServiceBusy(5),
            ProviderFailure.QuotaExhausted(null) to AiFailure.ServiceBusy(),
            ProviderFailure.Loading(9) to AiFailure.ServiceBusy(9),
            ProviderFailure.Overloaded to AiFailure.ServiceBusy(),
            ProviderFailure.ModelNotFound to AiFailure.ServiceUnavailable,
            ProviderFailure.AccessRejected to AiFailure.ServiceUnavailable,
            ProviderFailure.NoCredits to AiFailure.ServiceUnavailable,
            ProviderFailure.ServerError to AiFailure.ServiceUnavailable,
            ProviderFailure.RequestRejected to AiFailure.ServiceUnavailable,
            ProviderFailure.ContentBlocked to AiFailure.InvalidResponse,
            ProviderFailure.MalformedResponse to AiFailure.InvalidResponse,
            ProviderFailure.Unknown to AiFailure.Unknown,
        )
        expected.forEach { (failure, category) -> assertEquals(category, failure.toAiFailure()) }
    }
}
