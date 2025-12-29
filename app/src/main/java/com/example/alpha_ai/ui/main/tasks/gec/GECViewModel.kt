package com.example.alpha_ai.ui.main.tasks.gec

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.core.constants.UserProvider
import com.example.alpha_ai.data.firebase.FireStoreUtils
import com.example.alpha_ai.data.models.History
import com.example.alpha_ai.data.remote.api.ApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//import com.example.alpha_ai.database.apis.ApiManager
//import com.example.alpha_ai.database.apis.GECModelApi
//import com.example.alpha_ai.database.apis.GECarModelApi
//import com.example.alpha_ai.database.apis.LDFModelApi
//import com.example.alpha_ai.database.apis.SUMModelApi
//import com.example.alpha_ai.database.apis.SUMarModelApi
//import com.example.alpha_ai.database.apis.UniversalTextApi

class GECViewModel : BaseViewModel<GECNavigator>(){

//    private val apiManager = ApiManager.getInstance(context)

    var input = ObservableField<String>()
    var output = ObservableField<String>()
    var loadingVisibility = ObservableField<Boolean>()
    var inputError = ObservableField<String?>()
    var outputError = ObservableField<String?>()
    lateinit var payload:String
    private var flag = false

    fun submit(){
        if(!validateForm()){
            return
        }
//        loadingVisibility.set(true)
//        navigator?.showLoading("Loading...")
//        payload = """{"inputs": "${input.get()}"}"""
//        LDFModelApi.query(payload) { response ->
//            Log.e("response", response ?: "Error: Response is null")
//            if (response.equals("en")){
//                payload = "${input.get()}"
//
//                UniversalTextApi.processText(
//                    UniversalTextApi.TaskType.GEC_ENGLISH,
//                    payload
//                ) { response ->
//                    output.set(response ?: "Error: Response is null")
//                    payload = """{"inputs": "${response}"}"""
//                    Log.e("output", response ?: "Error: Response is null")
//                    if(response != null){
//                        flag = true
//                    }
//                }
//            }
//            else if (response.equals("ar")){
//                GECarModelApi.getGECResponse(payload) { response ->
//                    output.set(response ?: "Error: Response is null")
//                    payload = """{"inputs": "${response}"}"""
//                    Log.e("outputAr", response ?: "Error: Response is null")
//                    if(response != null){
//                        flag = true
//                    }
//                }
//                payload = "${input.get()}"
//                UniversalTextApi.processText(
//                    UniversalTextApi.TaskType.GEC_ARABIC,
//                    payload
//                ) { response ->
//                    output.set(response ?: "Error: Response is null")
//                    payload = """{"inputs": "${response}"}"""
//                    Log.e("outputAr", response ?: "Error: Response is null")
//                    if(response != null){
//                        flag = true
//                    }
//                }
//            }
//            else if (response == null) {
//                output.set("Error: Response is null")
//            }
//            else {
//                output.set("Error: Enter another language")
//            }
//            if(flag){
//                insertHistoryToDatabase(UserProvider.user?.uid)
//                flag = false
//            }
//        }
//        viewModelScope.launch(Dispatchers.IO) {
//            apiManager.processText(
//                task = ApiManager.TaskType.GEC_ENGLISH,
//                inputText = payload
//            ).collect { result ->
//                _grammarResult.value = result
//                output.set(response ?: "Error Response")
//                flag = true
//
//                // Handle result with extension functions
//                result.onSuccess { correctedText ->
//                    println("Corrected: $correctedText")
//                }.onError { message, code ->
//                    println("Error: $message (Code: $code)")
//                }
//            }
//        }
//        navigator?.hideLoading()
//        loadingVisibility.set(false)

    }

    fun summarize() {
        if(!validateFormOutput()){
            return
        }
//        loadingVisibility.set(true)
////        navigator?.showLoading("Loading...")
////        payload = """{"inputs": "${output.get()}"}"""
//        LDFModelApi.query(payload) { response ->
//            Log.e("response", response ?: "Error: Response is null")
//            if (response.equals("en")){
//                Log.e("SUM", payload)
//                SUMModelApi.query(payload) { response ->
//                    output.set(response ?: "Error: Response is null")
//                    Log.e("SUM", response ?: "Error: Response is null")
//                }
//            }
//            else if (response.equals("ar")){
//                Log.e("SUMar", payload)
//                SUMarModelApi.query(payload) { response ->
//                    output.set(response ?: "Error: Response is null")
//                    Log.e("SUM", response ?: "Error: Response is null")
//                }
//            }
//            else if (response == null) {
//                output.set("Error: Response is null")
//            }
//            else {
//                output.set("Error: Enter another language")
//            }
//        }
////        navigator?.hideLoading()
//        loadingVisibility.set(false)
    }

    fun insertHistoryToDatabase(userID:String?){
        val history = History(
            uid = userID,
            input = input.get(),
            response = output.get(),
            taskType = "gec",
        )
        FireStoreUtils()
            .sendHistory(history)
            .addOnCompleteListener { task->
                navigator?.hideLoading()
                if(task.isSuccessful){
                    navigator?.showMessage("Successfully Correction.","")
                }else{
                    navigator?.showMessage(task.exception?.localizedMessage!!,"")
                }
            }
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

    private var isValidOut = true
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

    fun copy() {
        output.get().let {
            if(!it?.trim().isNullOrBlank()){
                navigator?.copy(it!!)
            }
        }
    }


}