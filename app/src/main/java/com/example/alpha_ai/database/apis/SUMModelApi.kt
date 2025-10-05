package com.example.alpha_ai.database.apis

import com.example.alpha_ai.constants.Constants
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread


class SUMModelApi {
    companion object{
        fun query(data: String, callback: (String?) -> Unit) {
            thread {
                val client = OkHttpClient()
                val requestBody = data.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val apiRequest = Request.Builder()
                    .url(Constants.SUM.API_URL)
                    .headers(Constants.SUM.headers.toHeaders())
                    .post(requestBody)
                    .build()

                try {
                    val apiResponse = client.newCall(apiRequest).execute()
                    val responseBody = apiResponse.body?.string()
                    val generatedText = if (responseBody != null) {
                        val jsonArray = JSONArray(responseBody)
                        jsonArray.getJSONObject(0).getString("summary_text")
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