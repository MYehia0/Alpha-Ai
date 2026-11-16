package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Runs one chat completion call and turns whatever happens into a [ProviderResult].
 * Shared by both providers, which speak the same chat format and differ only in how their
 * errors are classified.
 */
internal suspend fun executeChatCall(
    classifier: ErrorClassifier,
    call: suspend () -> Response<ResponseBody>,
): ProviderResult<ChatReply> =
    try {
        val response = call()
        if (response.isSuccessful) {
            parseChatBody(response, classifier)
        } else {
            val error = NetworkJson.parseObjectOrNull(response.errorBody()?.string())?.errorInfoOrNull()
            ProviderResult.Failure(classifier.classify(response.code(), response.headers(), error))
        }
    } catch (t: Throwable) {
        ProviderResult.Failure(t.toProviderFailure())
    }

private fun parseChatBody(
    response: Response<ResponseBody>,
    classifier: ErrorClassifier,
): ProviderResult<ChatReply> {
    val root = NetworkJson.parseObjectOrNull(response.body()?.string())
        ?: return ProviderResult.Failure(ProviderFailure.MalformedResponse)

    // A provider can accept the request (status 200) and still fail: the error is in the body.
    root.errorInfoOrNull()?.let { error ->
        return ProviderResult.Failure(classifier.classify(response.code(), response.headers(), error))
    }

    val dto = NetworkJson.decodeFromJsonElement<ChatCompletionResponseDto>(root)
    val choice = dto.choices.firstOrNull()
    val text = (choice?.message?.content as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    if (text.isNullOrBlank()) return ProviderResult.Failure(ProviderFailure.MalformedResponse)

    return ProviderResult.Success(
        ChatReply(
            text = text,
            servedBy = dto.model,
            truncated = choice.finishReason == FINISH_REASON_LENGTH,
        ),
    )
}
