package com.example.alpha_ai.feature.auth.introduction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.domain.auth.usecase.CompleteIntroductionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class IntroductionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val completeIntroduction: CompleteIntroductionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(IntroductionUiState(page = savedStateHandle[KEY_PAGE] ?: 0))
    val state: StateFlow<IntroductionUiState> = _state.asStateFlow()

    private val _effects = Channel<IntroductionEffect>(Channel.BUFFERED)
    val effects: Flow<IntroductionEffect> = _effects.receiveAsFlow()

    private var finishing = false

    fun onIntent(intent: IntroductionIntent) {
        when (intent) {
            IntroductionIntent.NextPage -> setPage(_state.value.page + 1)
            IntroductionIntent.PreviousPage -> setPage(_state.value.page - 1)
            is IntroductionIntent.GoToPage -> setPage(intent.page)
            IntroductionIntent.Skip, IntroductionIntent.Finish -> finish()
        }
    }

    private fun setPage(page: Int) {
        val bounded = page.coerceIn(0, IntroductionUiState.PAGE_COUNT - 1)
        savedStateHandle[KEY_PAGE] = bounded
        _state.update { it.copy(page = bounded) }
    }

    /** Skip and Finish both leave once; a failed save only means the introduction shows again. */
    private fun finish() {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            completeIntroduction()
            _effects.send(IntroductionEffect.ToSignIn)
        }
    }

    private companion object {
        const val KEY_PAGE = "page"
    }
}
