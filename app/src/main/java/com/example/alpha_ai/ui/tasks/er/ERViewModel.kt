package com.example.alpha_ai.ui.tasks.er

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.database.apis.GECModelApi
import com.example.alpha_ai.database.apis.SSTModelApi
import com.example.alpha_ai.database.apis.TTSModelApi
import java.io.File
import java.io.FileInputStream

class ERViewModel : BaseViewModel<ERNavigator>(){
    var output1 = ObservableField<String>()
    var output3 = ObservableField<String>()
    val output2 = MutableLiveData<ByteArray?>()

    lateinit var payload:String

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
            var text = response ?: "Error: Response is null"
            output1.set(text)
            payload = """{"inputs": "${text}"}"""
            GECModelApi.getGECResponse(payload) { response ->
                text = response ?: "Error: Response is null"
                output3.set(text)
                sendTextToTTS(text)
                Log.e("gec", text)
            }
            Log.e("SST", text)
        }
    }

    fun sendTextToTTS(text: String) {
        TTSModelApi.query(text) { response ->
            output2.postValue(response)
            if (response == null) {
                Log.e("TTS", "Error: Response is null")
            }
        }
    }

    fun copy() {
        output3.get().let {
            if(!it?.trim().isNullOrBlank()){
                navigator?.copy(it!!)
            }
        }
    }

}