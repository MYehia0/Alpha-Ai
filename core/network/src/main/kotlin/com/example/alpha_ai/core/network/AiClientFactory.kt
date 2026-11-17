package com.example.alpha_ai.core.network

import com.example.alpha_ai.core.network.internal.DefaultHuggingFaceClient
import com.example.alpha_ai.core.network.internal.DefaultOpenRouterClient
import com.example.alpha_ai.core.network.internal.HuggingFaceService
import com.example.alpha_ai.core.network.internal.OpenRouterService
import com.example.alpha_ai.core.network.internal.baseOkHttpClient
import com.example.alpha_ai.core.network.internal.providerRetrofit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Builds provider clients without dependency injection, for developer tools such as the
 * opt-in live check. The app itself gets its clients from the Hilt bindings.
 */
object AiClientFactory {

    fun openRouter(
        credentials: AiCredentials,
        baseUrl: String,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): OpenRouterClient {
        val service = providerRetrofit(baseOkHttpClient(), AiProvider.OPENROUTER, credentials, baseUrl)
            .create(OpenRouterService::class.java)
        return DefaultOpenRouterClient(service, credentials, ioDispatcher)
    }

    fun huggingFace(
        credentials: AiCredentials,
        baseUrl: String,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): HuggingFaceClient {
        val service = providerRetrofit(baseOkHttpClient(), AiProvider.HUGGING_FACE, credentials, baseUrl)
            .create(HuggingFaceService::class.java)
        return DefaultHuggingFaceClient(service, credentials, ioDispatcher)
    }
}
