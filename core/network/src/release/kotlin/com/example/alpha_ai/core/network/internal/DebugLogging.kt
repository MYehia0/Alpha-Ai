package com.example.alpha_ai.core.network.internal

import okhttp3.Interceptor

/** Release builds have no HTTP logging at all; the logging library is not even on the classpath. */
internal fun debugLoggingInterceptor(): Interceptor? = null
