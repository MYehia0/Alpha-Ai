package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.data.ai.fakes.AiTestHarness
import com.example.alpha_ai.core.data.ai.fakes.FakeChains
import com.example.alpha_ai.core.data.ai.fakes.failure
import com.example.alpha_ai.core.data.ai.fakes.reply
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.AudioClip
import com.example.alpha_ai.core.domain.ai.ImageInput
import com.example.alpha_ai.core.domain.ai.Language
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

private const val INPUT = "He go to school every day and he like it very much."
private const val FIXED = "He goes to school every day and he likes it very much."
private const val SECRET_KEY = "sk-or-v1-test-secret"

private typealias Answer = suspend (ChatRequest) -> ProviderResult<ChatReply>

private val good: Answer = { reply(FIXED) }
private fun fails(failure: ProviderFailure): Answer = { failure(failure) }
private val slow: Answer = {
    delay(20_000)
    reply(FIXED)
}

@OptIn(ExperimentalCoroutinesApi::class)
class FallbackRunnerTest {

    private fun TestScope.harness() = AiTestHarness(this, FakeChains("a", "b", "c"))

    @After
    fun tearDown() = Timber.uprootAll()

    @Test
    fun `a rate limited first choice falls back to the second`() = runTest {
        val ai = harness()
        ai.client.respondByModel("a" to fails(ProviderFailure.RateLimited(30)), "b" to good)

        assertEquals(AiResult.Success(FIXED), ai.grammar.correct(INPUT))
        assertEquals(listOf("a", "b"), ai.client.modelsCalled)
    }

    @Test
    fun `a loading or overloaded first choice falls back to the second`() = runTest {
        for (cause in listOf(ProviderFailure.Loading(20), ProviderFailure.Overloaded)) {
            val ai = harness()
            ai.client.respondByModel("a" to fails(cause), "b" to good)

            assertEquals(AiResult.Success(FIXED), ai.grammar.correct(INPUT))
            assertEquals(listOf("a", "b"), ai.client.modelsCalled)
        }
    }

    @Test
    fun `a first choice that no longer exists falls back to the second`() = runTest {
        val ai = harness()
        ai.client.respondByModel("b" to good)

        assertEquals(AiResult.Success(FIXED), ai.grammar.correct(INPUT))
        assertEquals(listOf("a", "b"), ai.client.modelsCalled)
    }

    @Test
    fun `a first choice that is too slow is abandoned at its limit`() = runTest {
        val ai = harness()
        ai.client.respondByModel("a" to slow, "b" to good)

        assertEquals(AiResult.Success(FIXED), ai.grammar.correct(INPUT))
        assertEquals(listOf("a", "b"), ai.client.modelsCalled)
        assertEquals(TimeLimits.TEXT.attemptMillis, currentTime)
    }

    @Test
    fun `unusable output from the first choice falls back to the second`() = runTest {
        val ai = harness()
        ai.client.respondByModel("a" to { reply("   ") }, "b" to good)

        assertEquals(AiResult.Success(FIXED), ai.grammar.correct(INPUT))
        assertEquals(listOf("a", "b"), ai.client.modelsCalled)
    }

    @Test
    fun `when every choice fails one classified failure is returned`() = runTest {
        val ai = harness()
        ai.client.respondByModel(
            "a" to fails(ProviderFailure.ServerError),
            "b" to fails(ProviderFailure.RateLimited(25)),
            "c" to { reply("") },
        )

        assertEquals(AiResult.Failure(AiFailure.ServiceBusy(retryAfterSeconds = 25)), ai.grammar.correct(INPUT))
        assertEquals(listOf("a", "b", "c"), ai.client.modelsCalled)
    }

    @Test
    fun `no model is tried twice in one request`() = runTest {
        val ai = harness()
        ai.client.handler = fails(ProviderFailure.ServerError)

        assertEquals(AiResult.Failure(AiFailure.ServiceUnavailable), ai.grammar.correct(INPUT))
        assertEquals(listOf("a", "b", "c"), ai.client.modelsCalled)
    }

    @Test
    fun `a connection lost during the call stops without a fallback`() = runTest {
        val ai = harness()
        ai.client.handler = fails(ProviderFailure.NoConnection)

        assertEquals(AiResult.Failure(AiFailure.NoConnection), ai.grammar.correct(INPUT))
        assertEquals(listOf("a"), ai.client.modelsCalled)
    }

    @Test
    fun `a model rate limited a moment ago is skipped then tried again after its cooldown`() = runTest {
        val ai = harness()
        ai.client.respondByModel("a" to fails(ProviderFailure.RateLimited(null)), "b" to good)
        ai.grammar.correct(INPUT)
        ai.client.requests.clear()

        assertEquals(AiResult.Success(FIXED), ai.grammar.correct(INPUT))
        assertEquals(listOf("b"), ai.client.modelsCalled)

        advanceTimeBy(60_001)
        ai.client.requests.clear()
        ai.client.respondByModel("a" to good)

        assertEquals(AiResult.Success(FIXED), ai.grammar.correct(INPUT))
        assertEquals(listOf("a"), ai.client.modelsCalled)
    }

    @Test
    fun `cancelling during a fallback attempt makes no further call`() = runTest {
        val ai = harness()
        ai.client.respondByModel("a" to fails(ProviderFailure.ServerError), "b" to { awaitCancellation() }, "c" to good)
        var delivered: AiResult<String>? = null

        val job = launch { delivered = ai.grammar.correct(INPUT) }
        advanceTimeBy(1_000)
        job.cancel()
        runCurrent()

        assertTrue(job.isCancelled)
        assertEquals(null, delivered)
        assertEquals(listOf("a", "b"), ai.client.modelsCalled)
    }

    @Test
    fun `a chain of slow models ends with timed out inside the total limit`() = runTest {
        val ai = harness()
        ai.client.handler = slow

        assertEquals(AiResult.Failure(AiFailure.TimedOut), ai.grammar.correct(INPUT))
        // Two full attempts fit; the third would have under three seconds, so it never starts.
        assertEquals(listOf("a", "b"), ai.client.modelsCalled)
        assertTrue(currentTime <= TimeLimits.TEXT.totalMillis)
    }

    @Test
    fun `image and speech requests get the longer limits`() = runTest {
        val ai = harness()
        ai.client.handler = {
            delay(20_000)
            reply("EXIT")
        }

        assertEquals(AiResult.Success("EXIT"), ai.imageText.extractText(ImageInput(byteArrayOf(1), "image/png")))
        assertEquals(
            AiResult.Success("EXIT"),
            ai.speech.transcribe(AudioClip(byteArrayOf(1), "audio/wav", 1_000), Language.ENGLISH),
        )
        assertEquals(listOf("a", "a"), ai.client.modelsCalled)
    }

    @Test
    fun `a spent daily limit answers busy at once with no call on the next request`() = runTest {
        val ai = harness()
        ai.client.handler = fails(ProviderFailure.QuotaExhausted(resetAtEpochMillis = null))

        // The rest of the chain is on the same provider, so it is skipped with the known wait.
        val busy = AiResult.Failure(AiFailure.ServiceBusy(retryAfterSeconds = 3_600))
        assertEquals(busy, ai.grammar.correct(INPUT))
        assertEquals(listOf("a"), ai.client.modelsCalled)

        ai.client.requests.clear()
        assertEquals(busy, ai.grammar.correct(INPUT))
        assertTrue(ai.client.requests.isEmpty())
        assertEquals(0L, currentTime)
    }

    @Test
    fun `two concurrent requests both complete`() = runTest {
        val ai = harness()
        ai.client.handler = {
            delay(500)
            reply(FIXED)
        }

        val first = async { ai.grammar.correct(INPUT) }
        val second = async { ai.grammar.correct(INPUT, Language.ENGLISH) }

        assertEquals(AiResult.Success(FIXED), first.await())
        assertEquals(AiResult.Success(FIXED), second.await())
        assertEquals(500L, currentTime)
    }

    @Test
    fun `diagnostics carry only the agreed fields and never content or a key`() = runTest {
        val lines = mutableListOf<String>()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                lines += message
                t?.let { lines += it.toString() }
            }
        })
        val ai = harness()
        ai.client.respondByModel("a" to fails(ProviderFailure.RateLimited(30)), "b" to good)

        ai.grammar.correct(INPUT)

        val attempt = Regex(
            "attempt capability=GRAMMAR provider=OPENROUTER model=\\S+ promptVersion=\\d+ " +
                "attempt=\\d+ outcome=\\w+ elapsedMs=\\d+",
        )
        val request = Regex("request capability=GRAMMAR promptVersion=\\d+ result=\\w+ totalMs=\\d+")
        assertEquals(3, lines.size)
        assertTrue(attempt.matches(lines[0]) && lines[0].contains("model=a") && lines[0].contains("outcome=RateLimited"))
        assertTrue(attempt.matches(lines[1]) && lines[1].contains("model=b") && lines[1].contains("outcome=SUCCESS"))
        assertTrue(request.matches(lines[2]) && lines[2].contains("result=SUCCESS"))
        for (line in lines) {
            assertFalse(line.contains(INPUT))
            assertFalse(line.contains(FIXED))
            assertFalse(line.contains("school"))
            assertFalse(line.contains(SECRET_KEY))
            assertFalse(line.contains("Bearer", ignoreCase = true))
        }
    }
}

/** SC-002: every capability with a fallback survives each cause of a first-choice failure. */
class CapabilityFallbackTest {

    private val english = "The city council met on Monday to discuss the new park. ".repeat(6)
    private val arabic = "اجتمع مجلس المدينة يوم الاثنين لمناقشة الحديقة الجديدة."
    private val image = ImageInput(byteArrayOf(1, 2, 3), "image/jpeg")
    private val clip = AudioClip(byteArrayOf(1, 2, 3), "audio/wav", durationMillis = 2_000)

    private val causes: Map<String, Answer> = mapOf(
        "rate limited" to fails(ProviderFailure.RateLimited(10)),
        "overloaded" to fails(ProviderFailure.Overloaded),
        "not found" to fails(ProviderFailure.ModelNotFound),
        "too slow" to { awaitCancellation() },
        "malformed" to { reply("", truncated = true) },
    )

    /** Runs [call] once per cause against the bundled chains and checks the second choice answered. */
    private fun survives(secondReply: String, call: suspend (AiTestHarness) -> AiResult<String>) = runTest {
        for ((name, firstAnswer) in causes) {
            val ai = AiTestHarness(this)
            var first: String? = null
            ai.client.handler = { request ->
                if (first == null) first = request.model
                if (request.model == first) firstAnswer(request) else reply(secondReply)
            }

            assertEquals(name, AiResult.Success(secondReply), call(ai))
            assertEquals(name, 2, ai.client.modelsCalled.distinct().size)
            assertEquals(name, 2, ai.client.requests.size)
        }
    }

    @Test
    fun `grammar correction survives a failing first choice`() =
        survives(FIXED) { it.grammar.correct(INPUT) }

    @Test
    fun `translation survives a failing first choice`() =
        survives(arabic) { it.translator.translate(INPUT, Language.ENGLISH, Language.ARABIC) }

    @Test
    fun `summarization survives a failing first choice`() =
        survives("The council discussed the new park.") { it.summarizer.summarize(english) }

    @Test
    fun `image text extraction survives a failing first choice`() =
        survives("EXIT") { it.imageText.extractText(image) }

    @Test
    fun `speech has one choice and returns a classified failure when it fails`() = runTest {
        val expected = mapOf(
            "rate limited" to AiFailure.ServiceBusy(retryAfterSeconds = 10),
            "overloaded" to AiFailure.ServiceBusy(),
            "not found" to AiFailure.ServiceUnavailable,
            "too slow" to AiFailure.TimedOut,
            "malformed" to AiFailure.InvalidResponse,
        )
        for ((name, answer) in causes) {
            val ai = AiTestHarness(this)
            ai.client.handler = answer

            assertEquals(name, AiResult.Failure(expected.getValue(name)), ai.speech.transcribe(clip))
            assertEquals(name, 1, ai.client.requests.size)
        }
    }
}
