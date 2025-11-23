package com.example.alpha_ai.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Event> : ViewModel() {
    private val _uiState = MutableStateFlow<State?>(null)
    val uiState: StateFlow<State?> = _uiState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    protected fun setState(state: State) {
        _uiState.value = state
    }

    protected fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    protected fun setError(error: String?) {
        _error.value = error
    }

    abstract fun onEvent(event: Event)

    protected fun <T> execute(
        onStart: () -> Unit = { setLoading(true) },
        onComplete: () -> Unit = { setLoading(false) },
        onError: (Throwable) -> Unit = { setError(it.message) },
        block: suspend () -> T
    ) {
        viewModelScope.launch {
            try {
                onStart()
                block()
            } catch (e: Exception) {
                onError(e)
            } finally {
                onComplete()
            }
        }
    }
}
