package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.common.IoDispatcher
import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ChatRequest
import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.HuggingFaceClient
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import javax.inject.Inject

/** Hugging Face Inference Providers. Chat uses the same wire format as OpenRouter. */
internal interface HuggingFaceService {

    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body body: ChatCompletionRequestDto): Response<ResponseBody>

    @Streaming
    @POST("v1/chat/completions")
    fun chatCompletionStream(@Body body: ChatCompletionRequestDto): Call<ResponseBody>

    /** Speech recognition: the body is the raw audio, typed by its `Content-Type`. */
    @POST("hf-inference/models/{model}")
    suspend fun transcribe(
        @Path("model", encoded = true) model: String,
        @Body audio: RequestBody,
    ): Response<ResponseBody>
}

/**
 * Turns a Hugging Face error into a [ProviderFailure], from the HTTP status and, where it
 * matters, the error body. See specs/002-ai-provider-layer/contracts/provider-api.md.
 */
internal object HuggingFaceErrorClassifier : ErrorClassifier {

    private const val MODEL_NOT_SUPPORTED = "model_not_supported"

    override fun classify(status: Int, headers: Headers, error: ErrorInfo?): ProviderFailure = when {
        status == 402 -> ProviderFailure.NoCredits
        status == 401 || status == 403 -> ProviderFailure.AccessRejected
        status == 404 -> ProviderFailure.ModelNotFound
        status == 400 && error?.codeName == MODEL_NOT_SUPPORTED -> ProviderFailure.ModelNotFound
        status == 429 -> ProviderFailure.RateLimited(headers.retryAfterSeconds())
        status == 503 && error?.estimatedSeconds != null -> ProviderFailure.Loading(error.estimatedSeconds)
        status == 503 -> ProviderFailure.Overloaded
        status in 500..599 -> ProviderFailure.ServerError
        status in 400..499 -> ProviderFailure.RequestRejected
        // An error body on a successful status: the request was accepted but not served.
        error?.codeName == MODEL_NOT_SUPPORTED -> ProviderFailure.ModelNotFound
        else -> ProviderFailure.Unknown
    }
}

internal class DefaultHuggingFaceClient @Inject constructor(
    private val service: HuggingFaceService,
    private val credentials: AiCredentials,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HuggingFaceClient {

    override suspend fun chat(request: ChatRequest): ProviderResult<ChatReply> {
        if (!credentials.hasToken(AiProvider.HUGGING_FACE)) {
            return ProviderResult.Failure(ProviderFailure.AccessRejected)
        }
        return executeChatCall(HuggingFaceErrorClassifier) {
            service.chatCompletion(request.toDto(stream = false))
        }
    }

    override fun chatStream(request: ChatRequest): Flow<ChatStreamEvent> {
        if (!credentials.hasToken(AiProvider.HUGGING_FACE)) return rejectedStream()
        return streamChat(HuggingFaceErrorClassifier, ioDispatcher) {
            service.chatCompletionStream(request.toDto(stream = true))
        }
    }

    override suspend fun transcribe(model: String, audio: ByteArray, mimeType: String): ProviderResult<String> {
        if (!credentials.hasToken(AiProvider.HUGGING_FACE)) {
            return ProviderResult.Failure(ProviderFailure.AccessRejected)
        }
        val mediaType = mimeType.toMediaTypeOrNull()
            ?: return ProviderResult.Failure(ProviderFailure.RequestRejected)
        return executeJsonCall(
            classifier = HuggingFaceErrorClassifier,
            call = { service.transcribe(model, audio.toRequestBody(mediaType)) },
            // A clip with no speech is transcribed as blank text, which is a valid result.
            parse = { root -> (root["text"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull },
        )
    }
}
