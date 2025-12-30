package com.example.alpha_ai.data.remote.api

import android.content.Context
import com.example.alpha_ai.core.constants.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class ApiManager private constructor(context: Context) {

    private val apiService: ApiService
    private val responseHandler = NetworkResponseHandler()

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        // Single Retrofit instance for Router API
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.ROUTER_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    // ============== LANGUAGE DETECTION ==============
    fun detectLanguage(text: String): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        val requestBody = JSONObject().apply {
            put("inputs", text)
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val result = responseHandler.safeApiCall {
            apiService.inferenceApi(
                Constants.LDF.ENDPOINT,
                Constants.LDF.AUTH_HEADER,
                requestBody
            )
        }

        emit(
            when (result) {
                is ApiResult.Success -> {
                    try {
                        var responseString = result.data.string()
                        // Handle wrapped array response
                        if (responseString.startsWith("[") && responseString.endsWith("]")) {
                            responseString = responseString.substring(1, responseString.length - 1)
                        }
                        val jsonArray = JSONArray(responseString)
                        val language = jsonArray.getJSONObject(0).getString("label")
                        ApiResult.Success(language)
                    } catch (e: Exception) {
                        ApiResult.Error(
                            "Failed to parse language detection response: ${e.message}",
                            -1,
                            e
                        )
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> result
            }
        )
    }

    // ============== MACHINE TRANSLATION ==============
    fun translate(text: String, sourceLang: String = "en", targetLang: String = "ar"): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        val requestBody = JSONObject().apply {
            put("inputs", text)
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val result = responseHandler.safeApiCall {
            apiService.inferenceApi(
                Constants.MT.ENDPOINT,
                Constants.MT.AUTH_HEADER,
                requestBody
            )
        }

        emit(
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val responseString = result.data.string()
                        val jsonArray = JSONArray(responseString)
                        val translatedText = jsonArray.getJSONObject(0).getString("translation_text")
                        ApiResult.Success(translatedText)
                    } catch (e: Exception) {
                        ApiResult.Error("Failed to parse translation response: ${e.message}", -1, e)
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> result
            }
        )
    }

    // ============== SUMMARIZATION ==============
    fun summarize(text: String, language: String = "en"): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        val endpoint = if (language == "ar") Constants.SUMar.ENDPOINT else Constants.SUM.ENDPOINT
        val authHeader = if (language == "ar") Constants.SUMar.AUTH_HEADER else Constants.SUM.AUTH_HEADER

        val requestBody = JSONObject().apply {
            put("inputs", text)
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val result = responseHandler.safeApiCall {
            apiService.inferenceApi(
                endpoint,
                authHeader,
                requestBody
            )
        }

        emit(
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val responseString = result.data.string()
                        val jsonArray = JSONArray(responseString)
                        val summary = jsonArray.getJSONObject(0).getString("summary_text")
                        ApiResult.Success(summary)
                    } catch (e: Exception) {
                        ApiResult.Error(
                            "Failed to parse summarization response: ${e.message}",
                            -1,
                            e
                        )
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> result
            }
        )
    }

    // ============== UNIVERSAL TEXT PROCESSING (LLM-BASED) ==============
    enum class TaskType(val systemInstruction: String) {
        GEC_ENGLISH("Fix the grammar and spelling of this text. Output ONLY the corrected text."),
        GEC_ARABIC("Correct the grammar and spelling of this Arabic text. Output ONLY the corrected Arabic text."),
        TRANSLATE_EN_AR("Translate this English text to Arabic. Output ONLY the translation."),
        SUMMARIZE("Summarize this text in a concise paragraph."),
        DETECT_LANG("Detect the language of this text. Output ONLY the language code (e.g., 'en', 'ar', 'fr')."),
        COMPLETE_TEXT("Continue writing this text creatively.")
    }

    fun processText(task: TaskType, inputText: String): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        val jsonBody = JSONObject().apply {
            put("model", Constants.TextLLM.MODEL_ID)
            put("max_tokens", 1000)
            put("stream", false)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "${task.systemInstruction}\n\nInput: $inputText")
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val result = responseHandler.safeApiCall {
            apiService.chatCompletion(
                Constants.TextLLM.AUTH_HEADER,
                requestBody
            )
        }

        emit(
            when (result) {
                is ApiResult.Success -> {
                    try {
                        val responseString = result.data.string()
                        val rootObject = JSONObject(responseString)
                        val content = rootObject.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        ApiResult.Success(content.trim())
                    } catch (e: Exception) {
                        ApiResult.Error(
                            "Failed to parse text processing response: ${e.message}",
                            -1,
                            e
                        )
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> result
            }
        )
    }

    companion object {
        @Volatile
        private var instance: ApiManager? = null

        fun getInstance(context: Context): ApiManager {
            return instance ?: synchronized(this) {
                instance ?: ApiManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
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