package com.example.alpha_ai.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

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
                    ApiResult.Error(
                        response.errorBody()?.string() ?: "Unknown error",
                        response.code()
                    )
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is IOException -> "Network error: ${e.message}"
                    else -> "Unexpected error: ${e.message}"
                }
                ApiResult.Error(errorMessage, -1)
            }
        }
    }
}
