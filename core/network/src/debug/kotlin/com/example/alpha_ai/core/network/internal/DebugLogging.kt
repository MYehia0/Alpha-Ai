package com.example.alpha_ai.core.network.internal

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug builds log the request line and the response status only. Bodies are never logged,
 * because they hold user content, and the key is redacted in case the level is ever raised.
 */
internal fun debugLoggingInterceptor(): Interceptor? =
    HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
    }
