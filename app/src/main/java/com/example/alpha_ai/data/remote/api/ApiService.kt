package com.example.alpha_ai.data.remote.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Universal Chat Completions endpoint (for LLM-based tasks)
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    // HuggingFace Inference API endpoint (for specialized models)
    @POST
    @Headers("Content-Type: application/json")
    suspend fun inferenceApi(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}