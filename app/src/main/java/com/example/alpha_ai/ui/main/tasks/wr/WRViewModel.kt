package com.example.alpha_ai.ui.main.tasks.wr

import android.graphics.Bitmap
import android.util.Log
import androidx.databinding.ObservableField
import com.example.alpha_ai.base.BaseViewModel
//import com.example.alpha_ai.database.apis.ApiManager
//import com.example.alpha_ai.database.apis.MTModelApi

//import com.example.alpha_ai.database.apis.TGModelApi
//import com.example.alpha_ai.database.apis.TextToImageApi

class WRViewModel : BaseViewModel<WRNavigator>(){
    var input = ObservableField<String>()
    var output_en_trans = ObservableField<String>()
    var output_ar_trans = ObservableField<String>()
    var output_en_disc = ObservableField<String>()
//    var output_ar_disc = ObservableField<String>()
    var generatedBitmap = ObservableField<Bitmap>()
    var loadingVisibility = ObservableField<Boolean>()
    var inputError = ObservableField<String?>()
    lateinit var payload_MT:String
    lateinit var payload_TG:String

    fun submit(){
        if(!validateForm()){
            return
        }
        loadingVisibility.set(true)
//        navigator?.showLoading("Loading...")
        output_en_trans.set(input.get())
        payload_MT = """{"inputs": "${input.get()}"}"""
//        MTModelApi.query(payload_MT) { response ->
//            output_ar_trans.set(response ?: "Error: Response is null")
//            Log.e("ar_trans", response ?: "Error: Response is null")
//        }
        payload_TG = """{"inputs": "give me this word(${input.get()}) in the sentence"}"""
//        TGModelApi.query(payload_TG) { response ->
//            var text = response ?: "Error: Response is null"
//            output_en_disc.set(text)
//            Log.e("en_disc", text)
//        }
//        TextToImageApi.query(payload_MT) { response ->
//            generatedBitmap.set(response)
//            Log.e("generatedBitmap", response.toString())
//        }
//        navigator?.hideLoading()
        loadingVisibility.set(false)

    }


    private var isValid = true
    fun validateForm(): Boolean {
        if(input.get()?.trim().isNullOrBlank()){
            isValid = false
            inputError.set("please enter input.")
        }
        else{
            isValid = true
            inputError.set(null)
        }
        return isValid
    }
}