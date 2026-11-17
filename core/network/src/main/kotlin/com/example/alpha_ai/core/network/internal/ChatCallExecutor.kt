package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.serialization.json.JsonObject
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
): ProviderResult<ChatReply> = executeJsonCall(classifier, call, ::parseChatReply)

/**
 * Runs one call whose reply is a JSON object and turns whatever happens into a
 * [ProviderResult]. [parse] returns `null` when the body does not hold a usable result.
 */
internal suspend fun <T> executeJsonCall(
    classifier: ErrorClassifier,
    call: suspend () -> Response<ResponseBody>,
    parse: (JsonObject) -> T?,
): ProviderResult<T> =
    try {
        val response = call()
        if (response.isSuccessful) {
            val root = NetworkJson.parseObjectOrNull(response.body()?.string())
            val error = root?.errorInfoOrNull()
            when {
                root == null -> ProviderResult.Failure(ProviderFailure.MalformedResponse)
                // A provider can accept the request (status 200) and still fail: the error
                // is in the body.
                error != null ->
                    ProviderResult.Failure(classifier.classify(response.code(), response.headers(), error))
                else -> parse(root)?.let { ProviderResult.Success(it) }
                    ?: ProviderResult.Failure(ProviderFailure.MalformedResponse)
            }
        } else {
            val error = NetworkJson.parseObjectOrNull(response.errorBody()?.string())?.errorInfoOrNull()
            ProviderResult.Failure(classifier.classify(response.code(), response.headers(), error))
        }
    } catch (t: Throwable) {
        ProviderResult.Failure(t.toProviderFailure())
    }

private fun parseChatReply(root: JsonObject): ChatReply? {
    val dto = NetworkJson.decodeFromJsonElement<ChatCompletionResponseDto>(root)
    val choice = dto.choices.firstOrNull()
    val text = (choice?.message?.content as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    if (text.isNullOrBlank()) return null

    return ChatReply(
        text = text,
        servedBy = dto.model,
        truncated = choice.finishReason == FINISH_REASON_LENGTH,
    )
}
