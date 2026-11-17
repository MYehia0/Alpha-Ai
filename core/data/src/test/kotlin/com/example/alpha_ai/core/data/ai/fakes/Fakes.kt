package com.example.alpha_ai.core.data.ai.fakes

import com.example.alpha_ai.core.data.ai.DefaultGrammarCorrector
import com.example.alpha_ai.core.data.ai.DefaultImageTextExtractor
import com.example.alpha_ai.core.data.ai.DefaultSpeechTranscriber
import com.example.alpha_ai.core.data.ai.DefaultSummarizer
import com.example.alpha_ai.core.data.ai.DefaultTranslator
import com.example.alpha_ai.core.data.ai.config.BundledModelChains
import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.config.ModelChains
import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.data.ai.engine.ModelInvoker
import com.example.alpha_ai.core.data.ai.engine.TextTaskRunner
import com.example.alpha_ai.core.data.ai.fallback.AiClock
import com.example.alpha_ai.core.data.ai.fallback.FallbackRunner
import com.example.alpha_ai.core.data.ai.fallback.ModelHealthTracker
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.ConnectivityChecker
import com.example.alpha_ai.core.network.ContentPart
import com.example.alpha_ai.core.network.OpenRouterClient
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

internal fun reply(text: String, truncated: Boolean = false): ProviderResult<ChatReply> =
    ProviderResult.Success(ChatReply(text = text, servedBy = null, truncated = truncated))

internal fun failure(failure: ProviderFailure): ProviderResult<ChatReply> = ProviderResult.Failure(failure)

/** Records every request and answers with [handler]. No network is involved. */
internal class FakeOpenRouterClient : OpenRouterClient {
    val requests = mutableListOf<ChatRequest>()
    var handler: suspend (ChatRequest) -> ProviderResult<ChatReply> = { reply("ok") }

    /** Answers by model identifier; a model with no entry is reported as not found. */
    fun respondByModel(vararg answers: Pair<String, suspend (ChatRequest) -> ProviderResult<ChatReply>>) {
        val byModel = answers.toMap()
        handler = { request ->
            byModel[request.model]?.invoke(request) ?: failure(ProviderFailure.ModelNotFound)
        }
    }

    val modelsCalled: List<String> get() = requests.map { it.model }

    override suspend fun chat(request: ChatRequest): ProviderResult<ChatReply> {
        requests += request
        return handler(request)
    }

    override fun chatStream(request: ChatRequest): Flow<ChatStreamEvent> = emptyFlow()
}

internal class FakeConnectivity(var online: Boolean = true) : ConnectivityChecker {
    override fun isOnline(): Boolean = online
}

internal class FakeChains(private val chains: Map<Capability, List<ModelChoice>>) : ModelChains {
    constructor(vararg models: String) : this(
        Capability.entries.associateWith {
            models.mapIndexed { index, id -> ModelChoice(AiProvider.OPENROUTER, id, rank = (index + 1) * 10) }
        },
    )

    override suspend fun chainFor(capability: Capability): List<ModelChoice> = chains[capability].orEmpty()
}

/** The whole data layer wired by hand on top of fakes and the test's virtual clock. */
internal class AiTestHarness(scope: TestScope, var chains: ModelChains = BundledModelChains()) {
    val client = FakeOpenRouterClient()
    val connectivity = FakeConnectivity()
    val clock = AiClock { scope.testScheduler.currentTime }
    val health = ModelHealthTracker(clock)
    private val dispatcher = StandardTestDispatcher(scope.testScheduler)

    val runner: FallbackRunner get() = FallbackRunner(chains, health, connectivity, clock)
    private val invoker = ModelInvoker(client)
    private val tasks: TextTaskRunner get() = TextTaskRunner(runner, invoker)

    val grammar get() = DefaultGrammarCorrector(tasks)
    val translator get() = DefaultTranslator(tasks)
    val summarizer get() = DefaultSummarizer(tasks)
    val imageText get() = DefaultImageTextExtractor(runner, invoker, dispatcher)
    val speech get() = DefaultSpeechTranscriber(runner, invoker, dispatcher)
}

internal val ChatRequest.userText: String
    get() = content.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }
