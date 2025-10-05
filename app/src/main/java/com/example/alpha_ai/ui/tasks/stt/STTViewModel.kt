package com.example.alpha_ai.ui.tasks.stt

import android.graphics.Bitmap
import android.util.Log
import androidx.databinding.ObservableField
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.database.apis.OCRModelApi
import com.example.alpha_ai.database.apis.SSTModelApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class STTViewModel : BaseViewModel<STTNavigator>(){
    var output = ObservableField<String>()
    var outputError = ObservableField<String?>()

    private fun convertFileToByteArray(filePath: String): ByteArray {
        val file = File(filePath)
        val bytes = ByteArray(file.length().toInt())
        val fis = FileInputStream(file)
        fis.read(bytes)
        fis.close()
        return bytes
    }

    fun sendAudioToSST(filePath: String) {
        val byteArray = convertFileToByteArray(filePath)
        SSTModelApi.query(byteArray) { response ->
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