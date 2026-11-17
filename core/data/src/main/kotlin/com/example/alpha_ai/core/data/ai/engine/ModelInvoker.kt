package com.example.alpha_ai.core.data.ai.engine

import com.example.alpha_ai.core.data.ai.config.ApiStyle
import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ContentPart
import com.example.alpha_ai.core.network.OpenRouterClient
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import javax.inject.Inject

/**
 * Calls the model a [ModelChoice] names, on whichever provider it lives. This is the one
 * place that knows which client serves which provider, so capabilities never do.
 */
internal class ModelInvoker @Inject constructor(
    private val openRouter: OpenRouterClient,
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
        val request = ChatRequest(
            model = choice.modelId,
            systemPrompt = systemPrompt,
            content = content,
            maxTokens = maxTokens,
            temperature = temperature,
        )
        return when (choice.provider) {
            AiProvider.OPENROUTER -> openRouter.chat(request)
            // Switched off in configuration; its client arrives with the configuration work.
            AiProvider.HUGGING_FACE -> ProviderResult.Failure(ProviderFailure.ModelNotFound)
        }
    }
}
