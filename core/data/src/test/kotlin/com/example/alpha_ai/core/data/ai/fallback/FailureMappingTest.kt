package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.data.ai.fakes.AiTestHarness
import com.example.alpha_ai.core.data.ai.fakes.FakeChains
import com.example.alpha_ai.core.data.ai.fakes.failure
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.InvalidInputReason
import com.example.alpha_ai.core.network.ProviderFailure
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

private const val INPUT = "He go to school every day and he like it very much."

class FailureMappingTest {

    @After
    fun tearDown() = Timber.uprootAll()

    @Test
    fun `a single failing model reports the category of its failure`() = runTest {
        val expected = mapOf(
            ProviderFailure.NoConnection to AiFailure.NoConnection,
            ProviderFailure.Timeout to AiFailure.TimedOut,
            ProviderFailure.RateLimited(7) to AiFailure.ServiceBusy(7),
            ProviderFailure.QuotaExhausted(null) to AiFailure.ServiceBusy(),
            ProviderFailure.Loading(15) to AiFailure.ServiceBusy(15),
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
        for ((cause, category) in expected) {
            val ai = AiTestHarness(this, FakeChains("only"))
            ai.client.handler = { failure(cause) }

            assertEquals(cause.toString(), AiResult.Failure(category), ai.grammar.correct(INPUT))
        }
    }

    @Test
    fun `retry is offered only where trying again can help`() {
        val retryable = listOf(
            AiFailure.NoConnection,
            AiFailure.TimedOut,
            AiFailure.ServiceBusy(),
            AiFailure.InvalidResponse,
            AiFailure.Unknown,
        )
        retryable.forEach { assertTrue(it.toString(), it.canRetry) }
        assertFalse(AiFailure.ServiceUnavailable.canRetry)
        InvalidInputReason.entries.forEach { assertFalse(it.name, AiFailure.InvalidInput(it).canRetry) }
    }

    @Test
    fun `a rejected key is reported as unavailable and never appears in the log`() = runTest {
        val key = "sk-or-v1-" + "0".repeat(16) // gitleaks:allow (fake test value)
        val lines = mutableListOf<String>()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                lines += message
            }
        })
        val ai = AiTestHarness(this, FakeChains("a", "b"))
        ai.client.handler = { failure(ProviderFailure.AccessRejected) }

        assertEquals(AiResult.Failure(AiFailure.ServiceUnavailable), ai.grammar.correct(INPUT))
        // The whole provider is set aside, so the second model is not asked with the same key.
        assertEquals(listOf("a"), ai.client.modelsCalled)
        assertTrue(lines.any { it.contains("outcome=AccessRejected") })
        assertTrue(lines.none { it.contains(key) || it.contains("sk-or-") || it.contains(INPUT) })
    }
}
