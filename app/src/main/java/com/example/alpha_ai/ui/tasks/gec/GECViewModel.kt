package com.example.alpha_ai.ui.tasks.gec

import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.constants.UserProvider
import com.example.alpha_ai.database.FireStoreUtils
import com.example.alpha_ai.database.apis.ApiManager
import com.example.alpha_ai.database.apis.GECModelApi
import com.example.alpha_ai.database.apis.GECarModelApi
import com.example.alpha_ai.database.apis.LDFModelApi
import com.example.alpha_ai.database.apis.SUMModelApi
import com.example.alpha_ai.database.apis.SUMarModelApi
import com.example.alpha_ai.database.models.User
import com.google.api.ResourceDescriptor.History
import org.json.JSONObject

class GECViewModel : BaseViewModel<GECNavigator>(){
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
        loadingVisibility.set(true)
//        navigator?.showLoading("Loading...")
        payload = """{"inputs": "${input.get()}"}"""
        LDFModelApi.query(payload) { response ->
            Log.e("response", response ?: "Error: Response is null")
            if (response.equals("eng_Latn")){
                payload = """{"inputs": "Fix the grammar: ${input.get()}"}"""
                GECModelApi.getGECResponse(payload) { response ->
                    output.set(response ?: "Error: Response is null")
                    payload = """{"inputs": "${response}"}"""
                    Log.e("output", response ?: "Error: Response is null")
                    if(response != null){
                        flag = true
                    }
                }
            }
            else if (response.equals("arb_Arab")){
                GECarModelApi.getGECResponse(payload) { response ->
                    output.set(response ?: "Error: Response is null")
                    payload = """{"inputs": "${response}"}"""
                    Log.e("outputAr", response ?: "Error: Response is null")
                    if(response != null){
                        flag = true
                    }
                }
            }
            else if (response == null) {
                output.set("Error: Response is null")
            }
            else {
                output.set("Error: Enter another language")
            }
            if(flag){
                insertHistoryToDatabase(UserProvider.user?.uid)
                flag = false
            }
        }
//        navigator?.hideLoading()
        loadingVisibility.set(false)

    }

    fun summarize() {
        if(!validateFormOutput()){
            return
        }
        loadingVisibility.set(true)
//        navigator?.showLoading("Loading...")
//        payload = """{"inputs": "${output.get()}"}"""
        LDFModelApi.query(payload) { response ->
            Log.e("response", response ?: "Error: Response is null")
            if (response.equals("eng_Latn")){
                Log.e("SUM", payload)
                SUMModelApi.query(payload) { response ->
                    output.set(response ?: "Error: Response is null")
                    Log.e("SUM", response ?: "Error: Response is null")
                }
            }
            else if (response.equals("arb_Arab")){
                Log.e("SUMar", payload)
                SUMarModelApi.query(payload) { response ->
                    output.set(response ?: "Error: Response is null")
                    Log.e("SUM", response ?: "Error: Response is null")
                }
            }
            else if (response == null) {
                output.set("Error: Response is null")
            }
            else {
                output.set("Error: Enter another language")
            }
        }
//        navigator?.hideLoading()
        loadingVisibility.set(false)
    }

    fun insertHistoryToDatabase(userID:String?){
        val history = com.example.alpha_ai.ui.history.History(
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