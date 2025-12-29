package com.example.alpha_ai.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkResponseHandler {

    suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    response.body()?.let {
                        ApiResult.Success(it)
                    } ?: ApiResult.Error("Empty response body", response.code())
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    ApiResult.Error(errorMessage, response.code())
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is UnknownHostException -> "No internet connection"
                    is SocketTimeoutException -> "Request timeout. Please try again"
                    is IOException -> "Network error: ${e.message}"
                    else -> "Unexpected error: ${e.message}"
                }
                ApiResult.Error(errorMessage, -1, e)
            }
        }
    }
}