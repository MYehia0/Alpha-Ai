package com.example.alpha_ai.core.network

import com.example.alpha_ai.core.network.internal.DefaultOpenRouterClient
import com.example.alpha_ai.core.network.internal.OpenRouterService
import com.example.alpha_ai.core.network.internal.baseOkHttpClient
import com.example.alpha_ai.core.network.internal.providerRetrofit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OpenRouterClientTest {

    private lateinit var server: MockWebServer
    private var token: String? = "sk-or-test-token"

    private val credentials = object : AiCredentials {
        override fun tokenFor(provider: AiProvider): String? =
            token.takeIf { provider == AiProvider.OPENROUTER }
    }

    private lateinit var client: OpenRouterClient

    private val request = ChatRequest(
        model = "vendor/model:free",
        systemPrompt = "Fix the grammar.",
        content = listOf(ContentPart.Text("<content>He go home.</content>")),
        maxTokens = 256,
        temperature = 0.0,
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val service = providerRetrofit(
            baseClient = baseOkHttpClient().newBuilder()
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build(),
            provider = AiProvider.OPENROUTER,
            credentials = credentials,
            baseUrl = server.url("/api/v1/").toString(),
        ).create(OpenRouterService::class.java)
        client = DefaultOpenRouterClient(service, credentials, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ---- success -------------------------------------------------------------------------

    @Test
    fun `text request returns the reply and sends the expected body`() = runBlocking {
        server.enqueue(json(200, successBody("He goes home.")))

        val result = client.chat(request)

        assertEquals(
            ProviderResult.Success(ChatReply("He goes home.", "vendor/model:free", truncated = false)),
            result,
        )
        val recorded = server.takeRequest()
        assertEquals("/api/v1/chat/completions", recorded.path)
        assertEquals("Bearer sk-or-test-token", recorded.getHeader("Authorization"))
        val body = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
        assertEquals("vendor/model:free", body.getValue("model").jsonPrimitive.content)
        assertEquals("false", body.getValue("stream").jsonPrimitive.content)
        assertEquals("256", body.getValue("max_tokens").jsonPrimitive.content)
        val messages = body.getValue("messages").jsonArray
        assertEquals("system", messages[0].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals("Fix the grammar.", messages[0].jsonObject.getValue("content").jsonPrimitive.content)
        assertEquals("user", messages[1].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals(
            "<content>He go home.</content>",
            messages[1].jsonObject.getValue("content").jsonPrimitive.content,
        )
    }

    @Test
    fun `image request sends a data url part`() = runBlocking {
        server.enqueue(json(200, successBody("HELLO")))

        client.chat(
            request.copy(
                content = listOf(ContentPart.Text("Read it."), ContentPart.Image("QUJD", "image/png")),
            ),
        )

        val parts = userContentParts()
        assertEquals("text", parts[0].jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals("image_url", parts[1].jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(
            "data:image/png;base64,QUJD",
            parts[1].jsonObject.getValue("image_url").jsonObject.getValue("url").jsonPrimitive.content,
        )
    }

    @Test
    fun `audio request sends an input audio part`() = runBlocking {
        server.enqueue(json(200, successBody("hello")))

        client.chat(request.copy(content = listOf(ContentPart.Audio("QUJD", "wav"))))

        val audio = userContentParts()[0].jsonObject
        assertEquals("input_audio", audio.getValue("type").jsonPrimitive.content)
        val inner = audio.getValue("input_audio").jsonObject
        assertEquals("QUJD", inner.getValue("data").jsonPrimitive.content)
        assertEquals("wav", inner.getValue("format").jsonPrimitive.content)
    }

    @Test
    fun `reply cut off by the token limit is marked truncated`() = runBlocking {
        server.enqueue(json(200, successBody("He goes", finishReason = "length")))

        val result = client.chat(request) as ProviderResult.Success

        assertTrue(result.value.truncated)
    }

    // ---- classification by status and error type ------------------------------------------

    @Test
    fun `429 is rate limited with the retry-after value`() {
        server.enqueue(json(429, errorBody(429, "rate_limit_exceeded")).addHeader("Retry-After", "42"))
        assertFailure(ProviderFailure.RateLimited(42))
    }

    @Test
    fun `429 with no requests remaining is quota exhausted with the reset time`() {
        server.enqueue(
            json(429, errorBody(429, "rate_limit_exceeded"))
                .addHeader("X-RateLimit-Remaining", "0")
                .addHeader("X-RateLimit-Reset", "1794960000000"),
        )
        assertFailure(ProviderFailure.QuotaExhausted(1794960000000))
    }

    @Test
    fun `503 is overloaded`() {
        server.enqueue(json(503, errorBody(503, "provider_overloaded")))
        assertFailure(ProviderFailure.Overloaded)
    }

    @Test
    fun `502 and 500 are server errors`() {
        server.enqueue(json(502, errorBody(502, "provider_unavailable")))
        assertFailure(ProviderFailure.ServerError)
        server.enqueue(json(500, errorBody(500, null)))
        assertFailure(ProviderFailure.ServerError)
    }

    @Test
    fun `404 is model not found`() {
        server.enqueue(json(404, errorBody(404, "not_found")))
        assertFailure(ProviderFailure.ModelNotFound)
    }

    @Test
    fun `401 and permission denied are access rejected`() {
        server.enqueue(json(401, errorBody(401, "authentication")))
        assertFailure(ProviderFailure.AccessRejected)
        server.enqueue(json(403, errorBody(403, "permission_denied")))
        assertFailure(ProviderFailure.AccessRejected)
    }

    @Test
    fun `402 is no credits`() {
        server.enqueue(json(402, errorBody(402, "payment_required")))
        assertFailure(ProviderFailure.NoCredits)
    }

    @Test
    fun `content filter and refusal are content blocked`() {
        server.enqueue(json(403, errorBody(403, "content_policy_violation")))
        assertFailure(ProviderFailure.ContentBlocked)
        server.enqueue(json(403, errorBody(403, "refusal")))
        assertFailure(ProviderFailure.ContentBlocked)
    }

    @Test
    fun `408 is timeout`() {
        server.enqueue(json(408, errorBody(408, null)))
        assertFailure(ProviderFailure.Timeout)
    }

    @Test
    fun `400 413 and 422 are request rejected`() {
        server.enqueue(json(400, errorBody(400, "context_length_exceeded")))
        assertFailure(ProviderFailure.RequestRejected)
        server.enqueue(json(413, errorBody(413, "payload_too_large")))
        assertFailure(ProviderFailure.RequestRejected)
        server.enqueue(json(422, errorBody(422, "unprocessable")))
        assertFailure(ProviderFailure.RequestRejected)
    }

    @Test
    fun `error status with a body that is not json is classified by status`() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("<html>busy</html>"))
        assertFailure(ProviderFailure.Overloaded)
    }

    @Test
    fun `status 200 with an error body is a failure`() {
        server.enqueue(json(200, errorBody(429, "rate_limit_exceeded")))
        assertFailure(ProviderFailure.RateLimited(null))
    }

    // ---- malformed replies ----------------------------------------------------------------

    @Test
    fun `body that does not parse is malformed`() {
        server.enqueue(json(200, "not json"))
        assertFailure(ProviderFailure.MalformedResponse)
    }

    @Test
    fun `reply with no choices is malformed`() {
        server.enqueue(json(200, """{"model":"m","choices":[]}"""))
        assertFailure(ProviderFailure.MalformedResponse)
    }

    @Test
    fun `reply with blank text is malformed`() {
        server.enqueue(json(200, successBody("   ")))
        assertFailure(ProviderFailure.MalformedResponse)
    }

    @Test
    fun `reply with a field of the wrong type is malformed`() {
        server.enqueue(json(200, """{"choices":"oops"}"""))
        assertFailure(ProviderFailure.MalformedResponse)
    }

    // ---- transport ------------------------------------------------------------------------

    @Test
    fun `connection dropped while reading the body is malformed`() {
        server.enqueue(
            json(200, successBody("He goes home.")).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )
        assertFailure(ProviderFailure.MalformedResponse)
    }

    @Test
    fun `no answer within the read timeout is timeout`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertFailure(ProviderFailure.Timeout)
    }

    @Test
    fun `unreachable host is no connection`() = runBlocking {
        server.shutdown()

        val result = client.chat(request)

        assertEquals(ProviderResult.Failure(ProviderFailure.NoConnection), result)
    }

    // ---- credentials and cancellation -----------------------------------------------------

    @Test
    fun `blank key is access rejected and sends nothing`() = runBlocking {
        token = " "

        val result = client.chat(request)

        assertEquals(ProviderResult.Failure(ProviderFailure.AccessRejected), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `cancelling the caller cancels the call and delivers nothing`() = runBlocking {
        server.enqueue(json(200, successBody("late")).setBodyDelay(5, TimeUnit.SECONDS))

        val call = async(Dispatchers.IO) { client.chat(request) }
        server.takeRequest(2, TimeUnit.SECONDS)
        call.cancel()

        try {
            withTimeout(2_000) { call.await() }
            fail("expected cancellation")
        } catch (_: CancellationException) {
            assertTrue(call.isCancelled)
        }
    }

    private fun assertFailure(expected: ProviderFailure) = runBlocking {
        assertEquals(ProviderResult.Failure(expected), client.chat(request))
    }

    private fun userContentParts() =
        Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            .getValue("messages").jsonArray[1].jsonObject.getValue("content").jsonArray

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun successBody(text: String, finishReason: String = "stop") =
        """{"model":"vendor/model:free","choices":[{"message":{"role":"assistant","content":"$text"},"finish_reason":"$finishReason"}]}"""

    private fun errorBody(code: Int, errorType: String?): String {
        val metadata = errorType?.let { ""","metadata":{"error_type":"$it"}""" }.orEmpty()
        return """{"error":{"code":$code,"message":"provider said no"$metadata}}"""
    }
}
