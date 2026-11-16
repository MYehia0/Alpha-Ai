package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import okhttp3.Interceptor
import okhttp3.Response

/** Adds the provider's bearer token to every request. Callers never handle a key. */
internal class AuthInterceptor(
    private val provider: AiProvider,
    private val credentials: AiCredentials,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = credentials.tokenFor(provider)
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}
