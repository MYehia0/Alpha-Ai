package com.example.alpha_ai.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    @POST
    @Headers("Content-Type: application/json")
    suspend fun postRequest(
        @Url url: String,
        @Body body: RequestBody,
        @retrofit2.http.Header("Authorization") authHeader: String
    ): Response<ResponseBody>
}
