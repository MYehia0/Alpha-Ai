package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.common.IoDispatcher
import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.OpenRouterClient
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

internal class DefaultOpenRouterClient @Inject constructor(
    private val service: OpenRouterService,
    private val credentials: AiCredentials,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OpenRouterClient {

    override suspend fun chat(request: ChatRequest): ProviderResult<ChatReply> {
        if (!credentials.hasToken(AiProvider.OPENROUTER)) {
            return ProviderResult.Failure(ProviderFailure.AccessRejected)
        }
        return executeChatCall(OpenRouterErrorClassifier) {
            service.chatCompletion(request.toDto(stream = false))
        }
    }

    override fun chatStream(request: ChatRequest): Flow<ChatStreamEvent> {
        if (!credentials.hasToken(AiProvider.OPENROUTER)) return rejectedStream()
        return streamChat(OpenRouterErrorClassifier, ioDispatcher) {
            service.chatCompletionStream(request.toDto(stream = true))
        }
    }
}

/** Whether [provider] has a key. A missing key is logged, by provider name only. */
internal fun AiCredentials.hasToken(provider: AiProvider): Boolean {
    val present = !tokenFor(provider).isNullOrBlank()
    if (!present) Timber.tag("AiNetwork").w("no key configured for %s; nothing was sent", provider)
    return present
}

/** The stream for a provider with no key: it fails at once and sends nothing. */
internal fun rejectedStream(): Flow<ChatStreamEvent> =
    flowOf(ChatStreamEvent.Error(ProviderFailure.AccessRejected))
