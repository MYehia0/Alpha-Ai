package com.example.alpha_ai.core.data.ai.engine

import com.example.alpha_ai.core.data.ai.config.ApiStyle
import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.ContentPart
import com.example.alpha_ai.core.network.HuggingFaceClient
import com.example.alpha_ai.core.network.OpenRouterClient
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Calls the model a [ModelChoice] names, on whichever provider it lives. This is the one
 * place that knows which client serves which provider, so capabilities never do.
 */
internal class ModelInvoker @Inject constructor(
    private val openRouter: OpenRouterClient,
    private val huggingFace: HuggingFaceClient,
) {

    /**
     * Sends [systemPrompt] and the user's [content] to the chosen model. Only the reply's
     * message text is returned; reasoning a model reports separately is never used.
     */
    suspend fun chat(
        choice: ModelChoice,
        systemPrompt: String,
        content: List<ContentPart>,
        maxTokens: Int,
        temperature: Double,
    ): ProviderResult<ChatReply> {
        if (choice.apiStyle != ApiStyle.CHAT) return ProviderResult.Failure(ProviderFailure.RequestRejected)
        val request = ChatRequest(choice.modelId, systemPrompt, content, maxTokens, temperature)
        return when (choice.provider) {
            AiProvider.OPENROUTER -> openRouter.chat(request)
            AiProvider.HUGGING_FACE -> huggingFace.chat(request)
        }
    }

    /** The same request as [chat], with the reply delivered as it is generated. */
    fun chatStream(
        choice: ModelChoice,
        systemPrompt: String,
        content: List<ContentPart>,
        maxTokens: Int,
        temperature: Double,
    ): Flow<ChatStreamEvent> {
        if (choice.apiStyle != ApiStyle.CHAT) return flowOf(ChatStreamEvent.Error(ProviderFailure.RequestRejected))
        val request = ChatRequest(choice.modelId, systemPrompt, content, maxTokens, temperature)
        return when (choice.provider) {
            AiProvider.OPENROUTER -> openRouter.chatStream(request)
            AiProvider.HUGGING_FACE -> huggingFace.chatStream(request)
        }
    }

    /** Transcribes raw audio with a speech recognition model ([ApiStyle.HF_SPEECH]). */
    suspend fun transcribe(choice: ModelChoice, audio: ByteArray, mimeType: String): ProviderResult<String> =
        if (choice.apiStyle == ApiStyle.HF_SPEECH && choice.provider == AiProvider.HUGGING_FACE) {
            huggingFace.transcribe(choice.modelId, audio, mimeType)
        } else {
            ProviderResult.Failure(ProviderFailure.RequestRejected)
        }
}
