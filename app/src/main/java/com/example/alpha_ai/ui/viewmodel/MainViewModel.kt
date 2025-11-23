package com.example.alpha_ai.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.network.ApiManager
import com.example.alpha_ai.network.ApiResult
import com.example.alpha_ai.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel : BaseViewModel<MainUiState, MainUiEvent>() {
    private lateinit var apiManager: ApiManager
    
    private val _grammarCorrectionResult = MutableStateFlow<String?>(null)
    val grammarCorrectionResult: StateFlow<String?> = _grammarCorrectionResult

    fun initialize(context: Context) {
        if (!::apiManager.isInitialized) {
            apiManager = ApiManager.getInstance(context)
        }
    }

    override fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.CorrectGrammar -> correctGrammar(event.text)
            // Add other events here
        }
    }

    private fun correctGrammar(text: String) {
        viewModelScope.launch {
            apiManager.correctGrammar(text).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        _grammarCorrectionResult.value = result.data
                        setState(MainUiState.Success(result.data))
                    }
                    is ApiResult.Error -> {
                        setError(result.message)
                        setState(MainUiState.Error(result.message))
                    }
                    is ApiResult.Loading -> {
                        setLoading(true)
                        setState(MainUiState.Loading)
                    }
                }
            }
        }
    }

    // Add other API call methods here following the same pattern
    // Example:
    // private fun translateText(text: String, sourceLang: String, targetLang: String) { ... }
    // private fun convertTextToSpeech(text: String) { ... }
}

sealed class MainUiEvent {
    data class CorrectGrammar(val text: String) : MainUiEvent()
    // Add other events here
    // Example:
    // data class TranslateText(val text: String, val sourceLang: String, val targetLang: String) : MainUiEvent()
    // data class ConvertTextToSpeech(val text: String) : MainUiEvent()
}

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val result: String) : MainUiState()
    data class Error(val message: String) : MainUiState()
}
