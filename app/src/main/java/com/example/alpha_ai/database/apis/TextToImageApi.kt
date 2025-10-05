package com.example.alpha_ai.database.apis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.alpha_ai.constants.Constants
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread

class TextToImageApi {
    companion object {
        fun query(text: String, callback: (Bitmap?) -> Unit) {
            thread {
                val client = OkHttpClient()

                val requestBody = text.toRequestBody("application/json".toMediaTypeOrNull())
                val apiRequest = Request.Builder()
                    .url(Constants.IG.API_URL)
                    .headers(Constants.IG.headers.toHeaders())
                    .post(requestBody)
                    .build()

                try {
                    val apiResponse = client.newCall(apiRequest).execute()
                    val responseBody = apiResponse.body?.bytes()
                    val generatedImage = if (responseBody != null) {
                        BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    } else {
                        null
                    }
                    callback(generatedImage)
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
