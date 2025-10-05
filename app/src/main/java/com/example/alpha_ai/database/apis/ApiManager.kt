package com.example.alpha_ai.database.apis

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

class ApiManager {
    companion object{
        private var retrofit: Retrofit?=null // (base usrl, converter(Gson))

        @Synchronized
        private fun getInstance(): Retrofit {
            if(retrofit == null){

                // loggingInterceptor
                val loggingInterceptor = HttpLoggingInterceptor{
                    Log.e("api",it)
                }
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build()

                // build retrofit object
                retrofit = Retrofit.Builder()
                    .baseUrl("https://api-inference.huggingface.co/")
                    .client(okHttpClient) // add Interceptor
                    .addConverterFactory(GsonConverterFactory.create()) // add Gson Converter
                    .build()
            }
            return retrofit!!
        }

        // create WebServices object
        fun getApis():WebServices{
            return getInstance().create(WebServices::class.java)
        }

    }

}

interface  WebServices { // abstract class
    @POST("models/gotutiyan/gec-t5-large-clang8")
    fun getCorrection(@Query("apiKey") apiKey: String, @Body inCorrected:InCorrected): Call<Generated>
}

data class Generated(

    @field:SerializedName("generated_text")
    val prediction: String? = null
)

data class InCorrected(

    @field:SerializedName("inputs")
    val input: String? = null
)