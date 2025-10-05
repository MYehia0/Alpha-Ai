package com.example.alpha_ai.ui.tasks.ocr

import android.graphics.Bitmap
import android.util.Log
import androidx.databinding.ObservableField
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.database.apis.LDFModelApi
import com.example.alpha_ai.database.apis.OCRModelApi
import com.example.alpha_ai.database.apis.SUMModelApi
import com.example.alpha_ai.database.apis.SUMarModelApi
import java.io.ByteArrayOutputStream

class OCRViewModel : BaseViewModel<OCRNavigator>(){
    var output = ObservableField<String>()
    var outputError = ObservableField<String?>()
    var loadingVisibility = ObservableField<Boolean>()

    var bitmap: Bitmap? = null

    fun submit(){
        if(bitmap == null){
            return
        }
        loadingVisibility.set(true)
//        navigator?.showLoading("Loading...")
        sendImageToOCR(bitmap!!)
//        navigator?.hideLoading()
        loadingVisibility.set(false)
    }

    fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun sendImageToOCR(bitmap: Bitmap) {
        val byteArray = convertBitmapToByteArray(bitmap)
        OCRModelApi.query(byteArray) { response ->
            output.set(response ?: "Error: Response is null")
            Log.e("output", response ?: "Error: Response is null")
        }
    }

    fun copy() {
        output.get().let {
            if(!it?.trim().isNullOrBlank()){
                navigator?.copy(it!!)
            }
        }
    }

    private var isValidOut = true
    fun correct() {
        if(!validateFormOutput()){
            return
        }
        navigator?.correct()
    }

    fun validateFormOutput(): Boolean {
        if(output.get()?.trim().isNullOrBlank()){
            isValidOut = false
            outputError.set("please enter output.")
        }
        else{
            isValidOut = true
            outputError.set(null)
        }
        return isValidOut
    }



}