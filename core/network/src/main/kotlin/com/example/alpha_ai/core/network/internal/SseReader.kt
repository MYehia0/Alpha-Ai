package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.ChatStreamEvent
import com.example.alpha_ai.core.network.ProviderFailure
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.ResponseBody
import okio.BufferedSource
import retrofit2.Call

private const val DATA_PREFIX = "data:"
private const val COMMENT_PREFIX = ":"
private const val DONE_MARKER = "[DONE]"

/**
 * Runs a streaming chat call and delivers it as [ChatStreamEvent]s, ending with exactly one
 * [ChatStreamEvent.Done] or [ChatStreamEvent.Error].
 *
 * The body is read on [ioDispatcher]. When the collector is cancelled the HTTP call is
 * cancelled too, which closes the connection even while a read is blocked.
 */
internal fun streamChat(
    classifier: ErrorClassifier,
    ioDispatcher: CoroutineDispatcher,
    newCall: () -> Call<ResponseBody>,
): Flow<ChatStreamEvent> = callbackFlow {
    val call = newCall()
    launch(ioDispatcher) {
        val terminal = try {
            readStream(call, classifier) { send(it) }
        } catch (t: Throwable) {
            ChatStreamEvent.Error(t.toProviderFailure())
        }
        send(terminal)
        close()
    }
    awaitClose { call.cancel() }
}.buffer()

/** Emits each delta through [onDelta] and returns the terminal event. */
private suspend fun readStream(
    call: Call<ResponseBody>,
    classifier: ErrorClassifier,
    onDelta: suspend (ChatStreamEvent.Delta) -> Unit,
): ChatStreamEvent {
    val response = call.execute()
    if (!response.isSuccessful) {
        val error = NetworkJson.parseObjectOrNull(response.errorBody()?.string())?.errorInfoOrNull()
        return ChatStreamEvent.Error(classifier.classify(response.code(), response.headers(), error))
    }
    val body = response.body() ?: return ChatStreamEvent.Error(ProviderFailure.MalformedResponse)

    return body.use {
        val source: BufferedSource = it.source()
        var finishReason: String? = null
        var sawText = false

        while (true) {
            val line = source.readUtf8Line() ?: break
            if (line.isEmpty() || line.startsWith(COMMENT_PREFIX) || !line.startsWith(DATA_PREFIX)) continue

            val data = line.removePrefix(DATA_PREFIX).trim()
            if (data == DONE_MARKER) {
                return@use if (sawText) {
                    ChatStreamEvent.Done(truncated = finishReason == FINISH_REASON_LENGTH)
                } else {
                    ChatStreamEvent.Error(ProviderFailure.MalformedResponse)
                }
            }

            val chunk = NetworkJson.parseObjectOrNull(data)
                ?: return@use ChatStreamEvent.Error(ProviderFailure.MalformedResponse)
            chunk.errorInfoOrNull()?.let { error ->
                return@use ChatStreamEvent.Error(classifier.classify(response.code(), response.headers(), error))
            }

            val choice = NetworkJson.decodeFromJsonElement<ChatCompletionResponseDto>(chunk).choices.firstOrNull()
            choice?.finishReason?.let { reason -> finishReason = reason }
            val text = choice?.delta?.content
            if (!text.isNullOrEmpty()) {
                sawText = true
                onDelta(ChatStreamEvent.Delta(text))
            }
        }

        // The stream ended without its end marker. A reply that reported its finish is
        // complete; anything else was cut off.
        if (sawText && finishReason != null) {
            ChatStreamEvent.Done(truncated = finishReason == FINISH_REASON_LENGTH)
        } else {
            ChatStreamEvent.Error(ProviderFailure.MalformedResponse)
        }
    }
}
