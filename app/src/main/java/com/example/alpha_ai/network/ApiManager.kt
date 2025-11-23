package com.example.alpha_ai.network

import android.content.Context
import com.example.alpha_ai.constants.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api-inference.huggingface.co/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    // GEC (Grammar Error Correction)
    suspend fun correctGrammar(text: String): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)
        val requestBody = JSONObject().apply {
            put("inputs", text)
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val result = responseHandler.safeApiCall {
            apiService.postRequest(
                Constants.GEC.MODEL_PATH,
                requestBody,
                "Bearer ${Constants.GEC.API_TOKEN}"
            )
        }

        emit(
            when (result) {
                is ApiResult.Success -> {
                    val jsonArray = JSONArray(result.data.string())
                    val correctedText = jsonArray.getJSONObject(0).getString("generated_text")
                    ApiResult.Success(correctedText)
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> result
            }
        )
    }

    // Add other API methods here following the same pattern
    // Example: translateText, convertTextToSpeech, etc.

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
