package com.example.alpha_ai.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Calls models through Hugging Face Inference Providers.
 *
 * No function throws, except `CancellationException`. Every other outcome is a
 * [ProviderResult] or a terminal [ChatStreamEvent]. One call is one HTTP request: nothing is
 * retried here. The access token is added from [AiCredentials]; a blank token yields
 * [ProviderFailure.AccessRejected] without sending anything.
 */
interface HuggingFaceClient {
    suspend fun chat(request: ChatRequest): ProviderResult<ChatReply>

    fun chatStream(request: ChatRequest): Flow<ChatStreamEvent>

    /** Transcribes [audio] with a speech recognition [model]. */
    suspend fun transcribe(model: String, audio: ByteArray, mimeType: String): ProviderResult<String>
}
