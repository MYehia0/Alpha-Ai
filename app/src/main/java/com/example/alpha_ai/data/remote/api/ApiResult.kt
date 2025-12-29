package com.example.alpha_ai.data.remote.api

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = -1, val exception: Throwable? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: Exception(message)
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, Int) -> Unit): ApiResult<T> {
        if (this is Error) action(message, code)
        return this
    }

    inline fun onLoading(action: () -> Unit): ApiResult<T> {
        if (this is Loading) action()
        return this
    }
}