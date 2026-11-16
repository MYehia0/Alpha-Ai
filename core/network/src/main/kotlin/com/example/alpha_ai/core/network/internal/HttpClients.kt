package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_SECONDS = 5L
private const val READ_TIMEOUT_SECONDS = 30L
private const val WRITE_TIMEOUT_SECONDS = 30L

/**
 * The client every provider client is derived from, so they share one connection pool.
 * Per-attempt and total time limits are enforced by the caller with coroutine timeouts; these
 * are the transport-level bounds.
 */
internal fun baseOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply { debugLoggingInterceptor()?.let(::addInterceptor) }
        .build()

/** A Retrofit instance for one provider, with that provider's token added to each request. */
internal fun providerRetrofit(
    baseClient: OkHttpClient,
    provider: AiProvider,
    credentials: AiCredentials,
    baseUrl: String,
): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(
            baseClient.newBuilder()
                .addInterceptor(AuthInterceptor(provider, credentials))
                .build(),
        )
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .build()
