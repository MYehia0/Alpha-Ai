package com.example.alpha_ai.core.network.internal

import com.example.alpha_ai.core.network.ProviderFailure
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns an exception thrown while calling a provider into a [ProviderFailure].
 * Cancellation is never classified: it is rethrown so structured concurrency keeps working.
 */
internal fun Throwable.toProviderFailure(): ProviderFailure = when (this) {
    is CancellationException -> throw this
    is UnknownHostException, is ConnectException, is NoRouteToHostException ->
        ProviderFailure.NoConnection
    is SocketTimeoutException -> ProviderFailure.Timeout
    // The connection dropped while the reply was being read.
    is IOException -> ProviderFailure.MalformedResponse
    is SerializationException -> ProviderFailure.MalformedResponse
    else -> ProviderFailure.Unknown
}
