package com.example.alpha_ai.ui.main.tasks.gec

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.data.firebase.FireStoreUtils
import com.example.alpha_ai.data.models.History
import com.example.alpha_ai.data.remote.api.ApiManager
import com.example.alpha_ai.data.remote.api.ApiResult
import kotlinx.coroutines.launch

class GECViewModelFactory(private val apiManager: ApiManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GECViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GECViewModel(apiManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class GECViewModel(private val apiManager: ApiManager) : BaseViewModel<GECNavigator>() {

    var input = ObservableField("")
    var output = ObservableField("")
    var loadingVisibility = ObservableField(false)
    var inputError = ObservableField("")
    var outputError = ObservableField("")

    private var detectedLanguage: String? = null

    fun submit() {
        if (!validateForm()) {
            return
        }

        val inputText = input.get() ?: return

        viewModelScope.launch {
            loadingVisibility.set(true)
            detectLanguageAndCorrect(inputText)
        }
    }

    private suspend fun detectLanguageAndCorrect(text: String) {
        apiManager.detectLanguage(text).collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    detectedLanguage = result.data
                    correctGrammar(text)
                }
                is ApiResult.Error -> {
                    loadingVisibility.set(false)
                    outputError.set("Error: ${result.message}")
                }
            }
        }
    }
    private suspend fun correctGrammar(text: String) {
        val task = when {
            detectedLanguage?.contains("ar", ignoreCase = true) == true -> ApiManager.TaskType.GEC_ARABIC
            detectedLanguage?.contains("en", ignoreCase = true) == true -> ApiManager.TaskType.GEC_ENGLISH
            else -> ApiManager.TaskType.GEC_ENGLISH
        }

        apiManager.processText(task, text).collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    output.set(result.data)
                    loadingVisibility.set(false)
//                    insertHistoryToDatabase(UserProvider.user?.uid)
                }
                is ApiResult.Error -> {
                    loadingVisibility.set(false)
                    outputError.set("Error: ${result.message}")
                }
            }
        }
    }

    fun summarize() {
        if (!validateFormOutput()) {
            return
        }

        val outputText = output.get() ?: return

        viewModelScope.launch {
            loadingVisibility.set(true)
            summarizeText(outputText)
        }
    }

    private suspend fun summarizeText(text: String) {
        apiManager.summarize(text, (detectedLanguage ?: "en") ).collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    output.set(result.data)
                    loadingVisibility.set(false)
                }
                is ApiResult.Error -> {
                    loadingVisibility.set(false)
                    outputError.set("Error: ${result.message}")
                }
            }
        }
    }

    private fun insertHistoryToDatabase(userID: String?) {
        val history = History(
            uid = userID,
            input = input.get(),
            response = output.get(),
            taskType = "gec",
        )

        FireStoreUtils()
            .sendHistory(history)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
//                    navigator?.showMessage("Successfully corrected.", "")
                } else {
//                    navigator?.showMessage(
//                        task.exception?.localizedMessage ?: "Failed to save history",
//                        ""
//                    )
                }
            }
    }

    fun copy() {
        output.get()?.let {
            if (it.trim().isNotBlank()) {
                navigator?.copy(it)
            }
        }
    }

    private fun validateForm(): Boolean {
        return if (input.get()?.trim().isNullOrBlank()) {
            inputError.set("Please enter input.")
            false
        } else {
            inputError.set(null)
            true
        }
    }

    fun paste(){}
    fun clear(){}
    fun share(){}

    private fun validateFormOutput(): Boolean {
        return if (output.get()?.trim().isNullOrBlank()) {
            outputError.set("Please generate output first.")
            false
        } else {
            outputError.set(null)
            true
        }
    }
}