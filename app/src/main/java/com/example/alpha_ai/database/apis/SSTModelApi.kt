package com.example.alpha_ai.database.apis

import android.graphics.Bitmap
import android.util.Log
import com.example.alpha_ai.constants.Constants
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.concurrent.thread

class SSTModelApi {
    companion object{
        fun query(audioByteArray: ByteArray, callback: (String?) -> Unit) {
            thread {
                val client = OkHttpClient()

                val requestBody = audioByteArray.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                val apiRequest = Request.Builder()
                    .url(Constants.SST.API_URL)
                    .headers(Constants.SST.headers.toHeaders())
                    .post(requestBody)
                    .build()

                try {
                    val apiResponse = client.newCall(apiRequest).execute()
                    val responseBody = apiResponse.body?.string()
                    Log.e("msg",responseBody.toString())
                    val generatedText = if (responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        jsonObject.getString("text")
                    } else {
                        null
                    }
                    callback(generatedText)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("msg",e.toString())
                    callback(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("msg",e.toString())
                    callback(null)
                }
            }
        }
    }
}
