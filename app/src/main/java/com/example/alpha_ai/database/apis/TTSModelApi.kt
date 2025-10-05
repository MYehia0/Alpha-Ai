package com.example.alpha_ai.database.apis

import android.util.Log
import com.example.alpha_ai.constants.Constants
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.concurrent.thread

class TTSModelApi {
    companion object {
        fun query(text: String, callback: (ByteArray?) -> Unit) {
            thread {
                val client = OkHttpClient()

                val requestBody = text.toRequestBody("application/json".toMediaTypeOrNull())
                val apiRequest = Request.Builder()
                    .url(Constants.TTS.API_URL)
                    .headers(Constants.TTS.headers.toHeaders())
                    .post(requestBody)
                    .build()

                try {
                    val apiResponse = client.newCall(apiRequest).execute()
                    val responseBody = apiResponse.body?.bytes()
                    callback(responseBody)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("msg", e.toString())
                    callback(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("msg", e.toString())
                    callback(null)
                }
            }
        }
    }
}
