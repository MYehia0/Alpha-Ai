package com.example.alpha_ai.core.network.internal

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** OpenRouter's HTTP API. The body is returned raw because errors can arrive with status 200. */
internal interface OpenRouterService {

    @POST("chat/completions")
    suspend fun chatCompletion(@Body body: ChatCompletionRequestDto): Response<ResponseBody>
}
