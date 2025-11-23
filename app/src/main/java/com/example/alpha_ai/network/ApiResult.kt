package com.example.alpha_ai.network

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading
}
