package com.example.alpha_ai.core.network

import com.example.alpha_ai.core.network.internal.DefaultOpenRouterClient
import com.example.alpha_ai.core.network.internal.OpenRouterService
import com.example.alpha_ai.core.network.internal.baseOkHttpClient
import com.example.alpha_ai.core.network.internal.providerRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChatStreamTest {

    private lateinit var server: MockWebServer
    private var token: String? = "sk-or-test-token"
    private val credentials = object : AiCredentials {
        override fun tokenFor(provider: AiProvider): String? = token
    }
    private lateinit var client: OpenRouterClient

    private val request = ChatRequest(
        model = "vendor/model:free",
        systemPrompt = "Fix the grammar.",
        content = listOf(ContentPart.Text("He go home.")),
        maxTokens = 256,
        temperature = 0.0,
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val service = providerRetrofit(
            baseClient = baseOkHttpClient().newBuilder().readTimeout(2, TimeUnit.SECONDS).build(),
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

    private fun events() = runBlocking { withTimeout(5_000) { client.chatStream(request).toList() } }

    private fun stream(vararg lines: String) =
        MockResponse().setHeader("Content-Type", "text/event-stream").setBody(lines.joinToString("") { "$it\n\n" })

    private fun delta(text: String, finishReason: String? = null): String {
        val finish = finishReason?.let { "\"$it\"" } ?: "null"
        return """data: {"choices":[{"delta":{"content":"$text"},"finish_reason":$finish}]}"""
    }

    @Test
    fun `deltas arrive in order and the stream ends with done`() {
        server.enqueue(stream(delta("He "), delta("goes "), delta("home."), delta("", "stop"), "data: [DONE]"))

        assertEquals(
            listOf(
                ChatStreamEvent.Delta("He "),
                ChatStreamEvent.Delta("goes "),
                ChatStreamEvent.Delta("home."),
                ChatStreamEvent.Done(truncated = false),
            ),
            events(),
        )
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("true", body.getValue("stream").jsonPrimitive.content)
    }

    @Test
    fun `comment lines are ignored`() {
        server.enqueue(stream(": OPENROUTER PROCESSING", delta("Hi"), ": keep-alive", delta("", "stop"), "data: [DONE]"))

        assertEquals(listOf(ChatStreamEvent.Delta("Hi"), ChatStreamEvent.Done(truncated = false)), events())
    }

    @Test
    fun `a reply cut off by the token limit ends as truncated`() {
        server.enqueue(stream(delta("He goes"), delta("", "length"), "data: [DONE]"))

        assertEquals(ChatStreamEvent.Done(truncated = true), events().last())
    }

    @Test
    fun `an error chunk after text ends the stream with the classified failure`() {
        server.enqueue(
            stream(
                delta("He "),
                """data: {"error":{"code":502,"message":"upstream failed","metadata":{"error_type":"provider_overloaded"}},"choices":[{"delta":{"content":""},"finish_reason":"error"}]}""",
                "data: [DONE]",
            ),
        )

        assertEquals(
            listOf(ChatStreamEvent.Delta("He "), ChatStreamEvent.Error(ProviderFailure.Overloaded)),
            events(),
        )
    }

    @Test
    fun `an error status is classified without any delta`() {
        server.enqueue(
            MockResponse().setResponseCode(429).setHeader("Retry-After", "12")
                .setBody("""{"error":{"code":429,"message":"slow down"}}"""),
        )

        assertEquals(listOf(ChatStreamEvent.Error(ProviderFailure.RateLimited(12))), events())
    }

    @Test
    fun `a connection dropped mid-stream ends with one error`() {
        server.enqueue(
            stream(delta("He "), delta("goes ")).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )

        val events = events()

        assertTrue(events.last() is ChatStreamEvent.Error)
        assertEquals(1, events.count { it is ChatStreamEvent.Done || it is ChatStreamEvent.Error })
    }

    @Test
    fun `a stream that ends with no text is malformed`() {
        server.enqueue(stream("data: [DONE]"))

        assertEquals(listOf(ChatStreamEvent.Error(ProviderFailure.MalformedResponse)), events())
    }

    @Test
    fun `a blank key fails at once and sends nothing`() {
        token = " "

        assertEquals(listOf(ChatStreamEvent.Error(ProviderFailure.AccessRejected)), events())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `cancelling the collector closes the connection`() = runBlocking {
        // A bare socket server: it sends one delta, keeps the reply open, and waits for the
        // client to hang up.
        val closedByClient = CountDownLatch(1)
        val socketServer = ServerSocket(0)
        val serverThread = thread {
            socketServer.accept().use { socket ->
                val input = socket.getInputStream()
                val head = StringBuilder()
                while (!head.endsWith("\r\n\r\n")) head.append(input.read().toChar())
                socket.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nConnection: close\r\n\r\n".toByteArray())
                    write((delta("He ") + "\n\n").toByteArray())
                    flush()
                }
                try {
                    while (input.read() != -1) Unit
                } catch (_: IOException) {
                    // A reset is also the client hanging up.
                }
                closedByClient.countDown()
            }
        }
        val service = providerRetrofit(
            baseClient = baseOkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).build(),
            provider = AiProvider.OPENROUTER,
            credentials = credentials,
            baseUrl = "http://localhost:${socketServer.localPort}/",
        ).create(OpenRouterService::class.java)
        val streamingClient = DefaultOpenRouterClient(service, credentials, Dispatchers.IO)

        try {
            // Taking the first event cancels the rest of the stream.
            val first = withTimeout(5_000) { streamingClient.chatStream(request).first() }

            assertEquals(ChatStreamEvent.Delta("He "), first)
            assertTrue("the connection stayed open", closedByClient.await(5, TimeUnit.SECONDS))
        } finally {
            socketServer.close()
            serverThread.join(2_000)
        }
    }
}
