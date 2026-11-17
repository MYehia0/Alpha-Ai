package com.example.alpha_ai.core.data.ai.fallback

import app.cash.turbine.test
import com.example.alpha_ai.core.data.ai.fakes.AiTestHarness
import com.example.alpha_ai.core.data.ai.fakes.FakeChains
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.InvalidInputReason
import com.example.alpha_ai.core.domain.ai.Language
import com.example.alpha_ai.core.domain.ai.TextStreamEvent
import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.ProviderFailure
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val INPUT = "He go to school every day and he like it very much."
private const val FIXED = "He goes to school every day and he likes it very much."

class StreamingFallbackRunnerTest {

    private fun TestScope.harness(streams: Map<String, Flow<ChatStreamEvent>>): AiTestHarness {
        val ai = AiTestHarness(this, FakeChains("a", "b"))
        ai.client.streamHandler = { request ->
            streams[request.model] ?: flow { emit(ChatStreamEvent.Error(ProviderFailure.ModelNotFound)) }
        }
        return ai
    }

    private fun pieces(vararg parts: String, end: ChatStreamEvent = ChatStreamEvent.Done(truncated = false)) =
        flow {
            parts.forEach { emit(ChatStreamEvent.Delta(it)) }
            emit(end)
        }

    @Test
    fun `text arrives as deltas and ends with the checked full text`() = runTest {
        val ai = harness(mapOf("a" to pieces("\"He goes to school ", "every day and he likes it very much.\"")))

        assertEquals(
            listOf(
                TextStreamEvent.Delta("\"He goes to school "),
                TextStreamEvent.Delta("every day and he likes it very much.\""),
                // The final text is cleaned, so it can differ from the deltas.
                TextStreamEvent.Completed(FIXED),
            ),
            ai.grammar.correctStreaming(INPUT).toList(),
        )
        assertEquals(listOf("a"), ai.client.modelsCalled)
    }

    @Test
    fun `a failure after text restarts on the next model`() = runTest {
        val ai = harness(
            mapOf(
                "a" to pieces("He goes ", end = ChatStreamEvent.Error(ProviderFailure.Overloaded)),
                "b" to pieces(FIXED),
            ),
        )

        assertEquals(
            listOf(
                TextStreamEvent.Delta("He goes "),
                TextStreamEvent.Restarted,
                TextStreamEvent.Delta(FIXED),
                TextStreamEvent.Completed(FIXED),
            ),
            ai.grammar.correctStreaming(INPUT).toList(),
        )
    }

    @Test
    fun `a failure before any text falls back without a restart`() = runTest {
        val ai = harness(
            mapOf(
                "a" to pieces(end = ChatStreamEvent.Error(ProviderFailure.RateLimited(20))),
                "b" to pieces(FIXED),
            ),
        )

        assertEquals(
            listOf(TextStreamEvent.Delta(FIXED), TextStreamEvent.Completed(FIXED)),
            ai.grammar.correctStreaming(INPUT).toList(),
        )
        // The rate-limited model is now cooling down for one-shot requests too.
        assertEquals(20L, ai.health.skipReason(FakeChains("a").chainFor(com.example.alpha_ai.core.data.ai.config.Capability.GRAMMAR).single())?.waitSeconds)
    }

    @Test
    fun `a final check that fails with no model left ends as failed`() = runTest {
        val arabic = "ذهب الولد إلى المدرسة كل يوم وهو يحبها كثيرا جدا"
        val ai = harness(mapOf("a" to pieces(arabic), "b" to pieces("He goes", end = ChatStreamEvent.Done(truncated = true))))

        val events = ai.grammar.correctStreaming(INPUT, Language.ENGLISH).toList()

        assertEquals(
            listOf(
                TextStreamEvent.Delta(arabic),
                TextStreamEvent.Restarted,
                TextStreamEvent.Delta("He goes"),
                TextStreamEvent.Failed(AiFailure.InvalidResponse),
            ),
            events,
        )
    }

    @Test
    fun `a stream that stalls is abandoned at the attempt limit`() = runTest {
        val ai = harness(
            mapOf(
                "a" to flow {
                    emit(ChatStreamEvent.Delta("He "))
                    awaitCancellation()
                },
                "b" to pieces(FIXED),
            ),
        )

        val events = ai.grammar.correctStreaming(INPUT).toList()

        assertEquals(TextStreamEvent.Restarted, events[1])
        assertEquals(TextStreamEvent.Completed(FIXED), events.last())
        assertEquals(TimeLimits.TEXT.attemptMillis, testScheduler.currentTime)
    }

    @Test
    fun `offline and invalid input fail at once with no call`() = runTest {
        val ai = harness(mapOf("a" to pieces(FIXED)))

        assertEquals(
            listOf(TextStreamEvent.Failed(AiFailure.InvalidInput(InvalidInputReason.EMPTY))),
            ai.summarizer.summarizeStreaming("  ").toList(),
        )
        ai.connectivity.online = false
        assertEquals(
            listOf(TextStreamEvent.Failed(AiFailure.NoConnection)),
            ai.translator.translateStreaming(INPUT, Language.ENGLISH, Language.ARABIC).toList(),
        )
        assertTrue(ai.client.requests.isEmpty())
    }

    @Test
    fun `cancelling the collector stops the stream and calls no further model`() = runTest {
        val ai = harness(
            mapOf(
                "a" to flow {
                    emit(ChatStreamEvent.Delta("He "))
                    delay(5_000)
                    emit(ChatStreamEvent.Error(ProviderFailure.ServerError))
                },
                "b" to pieces(FIXED),
            ),
        )

        ai.grammar.correctStreaming(INPUT).test {
            assertEquals(TextStreamEvent.Delta("He "), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("a"), ai.client.modelsCalled)
    }

    @Test
    fun `every stream ends with exactly one terminal event`() = runTest {
        val cases = listOf(
            mapOf("a" to pieces(FIXED)),
            mapOf("a" to pieces("He ", end = ChatStreamEvent.Error(ProviderFailure.ServerError))),
            mapOf("a" to flow<ChatStreamEvent> { emit(ChatStreamEvent.Delta("He ")) }),
            emptyMap(),
        )
        for (streams in cases) {
            val events = harness(streams).grammar.correctStreaming(INPUT).toList()
            val terminals = events.count { it is TextStreamEvent.Completed || it is TextStreamEvent.Failed }

            assertEquals(1, terminals)
            assertTrue(events.last() is TextStreamEvent.Completed || events.last() is TextStreamEvent.Failed)
        }
    }
}
