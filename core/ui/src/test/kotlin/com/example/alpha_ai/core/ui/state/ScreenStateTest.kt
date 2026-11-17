package com.example.alpha_ai.core.ui.state

import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.InvalidInputReason
import com.example.alpha_ai.core.ui.ai.messageRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenStateTest {

    private val retryable = listOf(
        AiFailure.NoConnection,
        AiFailure.TimedOut,
        AiFailure.ServiceBusy(),
        AiFailure.ServiceBusy(retryAfterSeconds = 30),
        AiFailure.InvalidResponse,
        AiFailure.Unknown,
    )

    private val notRetryable: List<AiFailure> =
        listOf(AiFailure.ServiceUnavailable) + InvalidInputReason.entries.map { AiFailure.InvalidInput(it) }

    @Test
    fun `retry is offered for failures that retrying can fix`() {
        retryable.forEach { assertTrue("$it", it.toErrorState().retryable) }
    }

    @Test
    fun `retry is not offered when retrying cannot help`() {
        notRetryable.forEach { assertFalse("$it", it.toErrorState().retryable) }
    }

    @Test
    fun `the error state uses the failure's localized message`() {
        (retryable + notRetryable).forEach { failure ->
            assertEquals("$failure", failure.messageRes(), failure.toErrorState().messageRes)
        }
    }

    @Test
    fun `retry follows the AI layer's own rule`() {
        (retryable + notRetryable).forEach { assertEquals(it.canRetry, it.isRetryable()) }
    }
}
