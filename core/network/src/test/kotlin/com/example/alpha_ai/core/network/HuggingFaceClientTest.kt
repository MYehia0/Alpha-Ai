package com.example.alpha_ai.core.network

import com.example.alpha_ai.core.network.internal.DefaultHuggingFaceClient
import com.example.alpha_ai.core.network.internal.HuggingFaceService
import com.example.alpha_ai.core.network.internal.baseOkHttpClient
import com.example.alpha_ai.core.network.internal.providerRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class HuggingFaceClientTest {

    private lateinit var server: MockWebServer
    private var token: String? = "hf_test_token" // gitleaks:allow (fake test value)
    private val credentials = object : AiCredentials {
        override fun tokenFor(provider: AiProvider): String? =
            token.takeIf { provider == AiProvider.HUGGING_FACE }
    }
    private lateinit var client: HuggingFaceClient

    private val request = ChatRequest(
        model = "vendor/chat-model",
        systemPrompt = "Fix the grammar.",
        content = listOf(ContentPart.Text("He go home.")),
        maxTokens = 256,
        temperature = 0.0,
    )
    private val audio = byteArrayOf(82, 73, 70, 70, 1, 2, 3)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val service = providerRetrofit(
            baseClient = baseOkHttpClient().newBuilder().readTimeout(500, TimeUnit.MILLISECONDS).build(),
            provider = AiProvider.HUGGING_FACE,
            credentials = credentials,
            baseUrl = server.url("/").toString(),
        ).create(HuggingFaceService::class.java)
        client = DefaultHuggingFaceClient(service, credentials, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun assertChatFailure(expected: ProviderFailure) = runBlocking {
        assertEquals(ProviderResult.Failure(expected), client.chat(request))
    }

    // ---- success -------------------------------------------------------------------------

    @Test
    fun `chat returns the reply and sends the token`() = runBlocking {
        server.enqueue(
            json(200, """{"model":"vendor/chat-model","choices":[{"message":{"content":"He goes home."},"finish_reason":"stop"}]}"""),
        )

        assertEquals(
            ProviderResult.Success(ChatReply("He goes home.", "vendor/chat-model", truncated = false)),
            client.chat(request),
        )
        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer hf_test_token", recorded.getHeader("Authorization")) // gitleaks:allow (fake test value)
    }

    @Test
    fun `transcription sends the raw audio with its content type`() = runBlocking {
        server.enqueue(json(200, """{"text":" good morning "}"""))

        val result = client.transcribe("openai/whisper-large-v3-turbo", audio, "audio/wav")

        assertEquals(ProviderResult.Success(" good morning "), result)
        val recorded = server.takeRequest()
        assertEquals("/hf-inference/models/openai/whisper-large-v3-turbo", recorded.path)
        assertEquals("audio/wav", recorded.getHeader("Content-Type"))
        assertEquals("Bearer hf_test_token", recorded.getHeader("Authorization")) // gitleaks:allow (fake test value)
        assertArrayEquals(audio, recorded.body.readByteArray())
    }

    @Test
    fun `transcription with no text member is malformed`() = runBlocking {
        server.enqueue(json(200, """{"chunks":[]}"""))

        assertEquals(
            ProviderResult.Failure(ProviderFailure.MalformedResponse),
            client.transcribe("openai/whisper-large-v3-turbo", audio, "audio/wav"),
        )
    }

    @Test
    fun `streaming chat delivers deltas`() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/event-stream").setBody(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n",
            ),
        )

        assertEquals(
            listOf(ChatStreamEvent.Delta("Hi"), ChatStreamEvent.Done(truncated = false)),
            client.chatStream(request).toList(),
        )
    }

    // ---- classification, one test per row ---------------------------------------------------

    @Test
    fun `402 with a bare string error is no credits`() {
        server.enqueue(json(402, """{"error":"Purchase pre-paid credits to continue using Inference Providers."}"""))
        assertChatFailure(ProviderFailure.NoCredits)
    }

    @Test
    fun `401 and 403 are access rejected`() {
        server.enqueue(json(401, """{"error":"Invalid credentials in Authorization header"}"""))
        assertChatFailure(ProviderFailure.AccessRejected)
        server.enqueue(json(403, """{"error":"Forbidden"}"""))
        assertChatFailure(ProviderFailure.AccessRejected)
    }

    @Test
    fun `404 is model not found`() {
        server.enqueue(json(404, """{"error":"Not Found"}"""))
        assertChatFailure(ProviderFailure.ModelNotFound)
    }

    @Test
    fun `400 with model not supported is model not found`() {
        server.enqueue(
            json(400, """{"error":{"message":"The requested model is not supported.","type":"invalid_request_error","code":"model_not_supported"}}"""),
        )
        assertChatFailure(ProviderFailure.ModelNotFound)
    }

    @Test
    fun `429 is rate limited with the stated wait`() {
        server.enqueue(json(429, """{"error":"Too Many Requests"}""").setHeader("Retry-After", "17"))
        assertChatFailure(ProviderFailure.RateLimited(17))
    }

    @Test
    fun `503 with an estimate is loading`() {
        server.enqueue(json(503, """{"error":"Model is currently loading","estimated_time":20.4}"""))
        assertChatFailure(ProviderFailure.Loading(21))
    }

    @Test
    fun `503 without an estimate is overloaded`() {
        server.enqueue(json(503, """{"error":"Service Unavailable"}"""))
        assertChatFailure(ProviderFailure.Overloaded)
    }

    @Test
    fun `500 502 and 504 are server errors`() {
        for (code in listOf(500, 502, 504)) {
            server.enqueue(json(code, "upstream error"))
            assertChatFailure(ProviderFailure.ServerError)
        }
    }

    @Test
    fun `another 4xx is request rejected`() {
        server.enqueue(json(422, """{"error":{"message":"bad input","type":"invalid_request_error"}}"""))
        assertChatFailure(ProviderFailure.RequestRejected)
    }

    @Test
    fun `a body that does not parse or has blank text is malformed`() {
        server.enqueue(json(200, "<html>gateway</html>"))
        assertChatFailure(ProviderFailure.MalformedResponse)
        server.enqueue(json(200, """{"choices":[{"message":{"content":"  "},"finish_reason":"stop"}]}"""))
        assertChatFailure(ProviderFailure.MalformedResponse)
    }

    // ---- credentials ------------------------------------------------------------------------

    @Test
    fun `a blank token sends nothing`() = runBlocking {
        token = ""

        assertEquals(ProviderResult.Failure(ProviderFailure.AccessRejected), client.chat(request))
        assertEquals(
            ProviderResult.Failure(ProviderFailure.AccessRejected),
            client.transcribe("openai/whisper-large-v3-turbo", audio, "audio/wav"),
        )
        assertEquals(
            listOf(ChatStreamEvent.Error(ProviderFailure.AccessRejected)),
            client.chatStream(request).toList(),
        )
        assertEquals(0, server.requestCount)
    }
}
