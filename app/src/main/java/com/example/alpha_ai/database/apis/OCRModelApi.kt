package com.example.alpha_ai.database.apis

import android.graphics.Bitmap
import android.util.Log
import com.example.alpha_ai.constants.Constant
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.concurrent.thread

class OCRModelApi {
    companion object{
        fun query(imageByteArray: ByteArray, callback: (String?) -> Unit) {
            thread {
                val client = OkHttpClient()

                val requestBody = imageByteArray.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                val apiRequest = Request.Builder()
                    .url(Constant.OCR.API_URL)
                    .headers(Constant.OCR.headers.toHeaders())
                    .post(requestBody)
                    .build()

                try {
                    val apiResponse = client.newCall(apiRequest).execute()
                    val responseBody = apiResponse.body?.string()
                    val generatedText = if (responseBody != null) {
                        val jsonArray = JSONArray(responseBody)
                        jsonArray.getJSONObject(0).getString("generated_text")
                    } else {
                        null
                    }
                    callback(generatedText)
                } catch (e: IOException) {
                    e.printStackTrace()
                    callback(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }
        }
    }
}
