package com.example.alpha_ai.database.apis

import android.graphics.Bitmap
import android.util.Log
import com.example.alpha_ai.constants.Constants
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.concurrent.thread

class TGModelApi {
    companion object{
        fun query(data: String, callback: (String?) -> Unit) {
            if (!isValidJson(data)) {
                Log.e("TGModelApi", "Invalid JSON data: $data")
                callback(null)
                return
            }
            thread {
                val client = OkHttpClient()
                val requestBody = data.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val apiRequest = Request.Builder()
                    .url(Constants.TG.API_URL)
                    .headers(Constants.TG.headers.toHeaders())
                    .post(requestBody)
                    .build()

                try {
                    Log.e("Sending JSON data: ", "$data")
                    val apiResponse = client.newCall(apiRequest).execute()
                    val responseBody = apiResponse.body?.string()
                    Log.e("msgT",responseBody!!)
                    val generatedText = if (responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            jsonArray.getJSONObject(0).getString("generated_text")
                        } catch (e: JSONException) {
                            Log.e("Failed to parse response:", "${e.message}")
                            null
                        }
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
        private fun isValidJson(json: String): Boolean {
            return try {
                JSONObject(json)
                true
            } catch (ex: JSONException) {
                try {
                    JSONArray(json)
                    true
                } catch (ex1: JSONException) {
                    false
                }
            }
        }

    }
}
