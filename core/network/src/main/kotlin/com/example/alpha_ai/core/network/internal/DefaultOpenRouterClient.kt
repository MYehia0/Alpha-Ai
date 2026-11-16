package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.OpenRouterClient
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class DefaultOpenRouterClient @Inject constructor(
    private val service: OpenRouterService,
    private val credentials: AiCredentials,
) : OpenRouterClient {

    override suspend fun chat(request: ChatRequest): ProviderResult<ChatReply> {
        if (credentials.tokenFor(AiProvider.OPENROUTER).isNullOrBlank()) {
            return ProviderResult.Failure(ProviderFailure.AccessRejected)
        }
        return executeChatCall(OpenRouterErrorClassifier) {
            service.chatCompletion(request.toDto(stream = false))
        }
    }

    override fun chatStream(request: ChatRequest): Flow<ChatStreamEvent> = flow {
        emitOneShot(chat(request))
    }
}

/**
 * Delivers a one-shot reply as a stream: the whole text in one piece, then the end.
 * It satisfies the stream contract for providers or builds without progressive delivery.
 */
internal suspend fun kotlinx.coroutines.flow.FlowCollector<ChatStreamEvent>.emitOneShot(
    result: ProviderResult<ChatReply>,
) {
    when (result) {
        is ProviderResult.Success -> {
            emit(ChatStreamEvent.Delta(result.value.text))
            emit(ChatStreamEvent.Done(result.value.truncated))
        }

        is ProviderResult.Failure -> emit(ChatStreamEvent.Error(result.failure))
    }
}
