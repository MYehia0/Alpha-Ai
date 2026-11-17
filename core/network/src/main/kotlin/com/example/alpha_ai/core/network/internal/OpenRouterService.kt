package com.example.alpha_ai.core.network.internal

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

/** OpenRouter's HTTP API. The body is returned raw because errors can arrive with status 200. */
internal interface OpenRouterService {

    @POST("chat/completions")
    suspend fun chatCompletion(@Body body: ChatCompletionRequestDto): Response<ResponseBody>

    /** The same call with the reply read as it arrives. A [Call] so it can be cancelled mid-read. */
    @Streaming
    @POST("chat/completions")
    fun chatCompletionStream(@Body body: ChatCompletionRequestDto): Call<ResponseBody>
}
