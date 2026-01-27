package com.example.alpha_ai.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.common.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    // Channel ensures single consumption (no re-trigger on config change)
    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    protected fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiEvent.close()
    }
}