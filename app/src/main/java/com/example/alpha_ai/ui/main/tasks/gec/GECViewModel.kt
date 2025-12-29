package com.example.alpha_ai.ui.main.tasks.gec

import android.content.Context
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.base.BaseViewModel
import com.example.alpha_ai.core.constants.UserProvider
import com.example.alpha_ai.data.firebase.FireStoreUtils
import com.example.alpha_ai.data.models.History
import com.example.alpha_ai.data.remote.api.ApiManager
import com.example.alpha_ai.data.remote.api.ApiResult
import kotlinx.coroutines.launch

/**
 * Alternative implementation that extends BaseViewModel
 * Pass context through initialization
 */
class GECViewModel(private val context: Context) : BaseViewModel<GECNavigator>() {

    private val apiManager = ApiManager.getInstance(context)

    // Observable fields for UI binding
    var input = ObservableField<String>()
    var output = ObservableField<String>()
    var loadingVisibility = ObservableField<Boolean>(false)
    var inputError = ObservableField<String?>()
    var outputError = ObservableField<String?>()

    private var detectedLanguage: String? = null

    /**
     * Main submit function - detects language then corrects grammar
     */
    fun submit() {
        if (!validateForm()) {
            return
        }

        val inputText = input.get() ?: return

        viewModelScope.launch {
            // Step 1: Show loading
            loadingVisibility.set(true)
//            navigator?.showLoading("Detecting language...")

            // Step 2: Detect language
            detectLanguageAndCorrect(inputText)
        }
    }

    /**
     * Detects language then corrects grammar based on detected language
     */
    private suspend fun detectLanguageAndCorrect(text: String) {
        apiManager.detectLanguage(text).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    Log.d("GECViewModel", "Detecting language...")
                }

                is ApiResult.Success -> {
                    detectedLanguage = result.data
                    Log.d("GECViewModel", "Detected language: ${result.data}")

                    // Update loading message
//                    navigator?.showLoading("Correcting grammar...")

                    // Correct grammar based on detected language
                    correctGrammar(text, result.data)
                }

                is ApiResult.Error -> {
                    Log.e("GECViewModel", "Language detection error: ${result.message}")
                    loadingVisibility.set(false)
//                    navigator?.hideLoading()
//                    navigator?.showMessage(
//                        "Failed to detect language: ${result.message}",
//                        ""
//                    )
                }
            }
        }
    }

    /**
     * Corrects grammar based on language
     */
    private suspend fun correctGrammar(text: String, language: String) {
        // Determine task type based on language
        val task = when {
            language.contains("ar", ignoreCase = true) -> ApiManager.TaskType.GEC_ARABIC
            language.contains("en", ignoreCase = true) -> ApiManager.TaskType.GEC_ENGLISH
            else -> ApiManager.TaskType.GEC_ENGLISH // Default to English
        }

        apiManager.processText(task, text).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    Log.d("GECViewModel", "Correcting grammar...")
                }

                is ApiResult.Success -> {
                    Log.d("GECViewModel", "Grammar corrected: ${result.data}")
                    output.set(result.data)
                    loadingVisibility.set(false)
//                    navigator?.hideLoading()

                    // Save to Firebase history
//                    insertHistoryToDatabase(UserProvider.user?.uid)
                }

                is ApiResult.Error -> {
                    Log.e("GECViewModel", "Grammar correction error: ${result.message}")
                    loadingVisibility.set(false)
//                    navigator?.hideLoading()
                    output.set("Error: ${result.message}")
//                    navigator?.showMessage(
//                        "Failed to correct grammar: ${result.message}",
//                        ""
//                    )
                }
            }
        }
    }

    /**
     * Summarize the output text
     */
    fun summarize() {
        if (!validateFormOutput()) {
            return
        }

        val outputText = output.get() ?: return

        viewModelScope.launch {
            loadingVisibility.set(true)
//            navigator?.showLoading("Summarizing...")

            // Detect language first, then summarize
            summarizeWithLanguageDetection(outputText)
        }
    }

    /**
     * Detects language then summarizes
     */
    private suspend fun summarizeWithLanguageDetection(text: String) {
        apiManager.detectLanguage(text).collect { languageResult ->
            when (languageResult) {
                is ApiResult.Loading -> {
                    Log.d("GECViewModel", "Detecting language for summarization...")
                }

                is ApiResult.Success -> {
                    val language = languageResult.data
                    Log.d("GECViewModel", "Detected language for summary: $language")

                    // Determine language code
                    val langCode = when {
                        language.contains("ar", ignoreCase = true) -> "ar"
                        else -> "en"
                    }

                    // Summarize
                    summarizeText(text, langCode)
                }

                is ApiResult.Error -> {
                    // If language detection fails, default to English
                    Log.w("GECViewModel", "Language detection failed, defaulting to English")
                    summarizeText(text, "en")
                }
            }
        }
    }

    /**
     * Summarizes text in the specified language
     */
    private suspend fun summarizeText(text: String, language: String) {
        apiManager.summarize(text, language).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    Log.d("GECViewModel", "Summarizing text...")
                }

                is ApiResult.Success -> {
                    Log.d("GECViewModel", "Summary: ${result.data}")
                    output.set(result.data)
                    loadingVisibility.set(false)
//                    navigator?.hideLoading()
                }

                is ApiResult.Error -> {
                    Log.e("GECViewModel", "Summarization error: ${result.message}")
                    loadingVisibility.set(false)
//                    navigator?.hideLoading()
//                    navigator?.showMessage(
//                        "Failed to summarize: ${result.message}",
//                        ""
//                    )
                }
            }
        }
    }

    /**
     * Insert history to Firebase
     */
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

    /**
     * Copy output to clipboard
     */
    fun copy() {
        output.get()?.let {
            if (it.trim().isNotBlank()) {
                navigator?.copy(it)
            }
        }
    }

    /**
     * Validate input form
     */
    private fun validateForm(): Boolean {
        return if (input.get()?.trim().isNullOrBlank()) {
            inputError.set("Please enter input.")
            false
        } else {
            inputError.set(null)
            true
        }
    }

    /**
     * Validate output form
     */
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