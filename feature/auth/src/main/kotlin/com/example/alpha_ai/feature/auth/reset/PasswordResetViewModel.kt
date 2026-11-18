package com.example.alpha_ai.feature.auth.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.usecase.PasswordResetResult
import com.example.alpha_ai.core.domain.auth.usecase.RequestPasswordResetUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Password reset (contracts/auth-screens.md). [initialEmail] is the route argument, typed on
 * sign-in. The cooldown is shared for the whole process, so reopening the screen within a minute
 * still shows the remaining wait.
 */
@HiltViewModel(assistedFactory = PasswordResetViewModel.Factory::class)
internal class PasswordResetViewModel @AssistedInject constructor(
    @Assisted initialEmail: String,
    private val savedStateHandle: SavedStateHandle,
    private val requestReset: RequestPasswordResetUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(initialEmail: String): PasswordResetViewModel
    }

    private val _state = MutableStateFlow(
        PasswordResetUiState(
            email = savedStateHandle[KEY_EMAIL] ?: initialEmail,
            sent = savedStateHandle[KEY_SENT] ?: false,
        ),
    )
    val state: StateFlow<PasswordResetUiState> = _state.asStateFlow()

    private val _effects = Channel<PasswordResetEffect>(Channel.BUFFERED)
    val effects: Flow<PasswordResetEffect> = _effects.receiveAsFlow()

    private var submitJob: Job? = null
    private var countdownJob: Job? = null

    init {
        startCountdown(requestReset.remainingCooldown())
    }

    fun onIntent(intent: PasswordResetIntent) {
        when (intent) {
            is PasswordResetIntent.EmailChanged -> {
                savedStateHandle[KEY_EMAIL] = intent.value
                _state.update {
                    it.copy(email = intent.value, fieldErrors = it.fieldErrors - FormField.Email, failure = null)
                }
            }
            PasswordResetIntent.Submit -> submit()
            PasswordResetIntent.Cancel -> cancel()
            PasswordResetIntent.BackToSignIn -> _effects.trySend(PasswordResetEffect.ToSignIn)
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        // The previous failure stays until the result arrives, so the layout does not shift under a
        // second tap and put Cancel where Submit was.
        _state.update { it.copy(submitting = true, cancellable = true) }
        submitJob = viewModelScope.launch {
            val result = requestReset(current.email)
            _state.update { it.copy(submitting = false, cancellable = false, failure = null) }
            when (result) {
                PasswordResetResult.Sent -> {
                    savedStateHandle[KEY_SENT] = true
                    _state.update { it.copy(sent = true) }
                    startCountdown(requestReset.remainingCooldown())
                }
                is PasswordResetResult.CoolingDown -> startCountdown(result.remaining)
                is PasswordResetResult.FieldErrors -> _state.update { it.copy(fieldErrors = result.errors) }
                is PasswordResetResult.Failure -> _state.update { it.copy(failure = result.failure) }
            }
        }
    }

    /** Stops the request and keeps the email; a reset that goes out anyway needs no clean-up. */
    private fun cancel() {
        if (!_state.value.cancellable) return
        submitJob?.cancel()
        submitJob = null
        _state.update { it.copy(submitting = false, cancellable = false, failure = null) }
    }

    private fun startCountdown(remaining: Duration) {
        countdownJob?.cancel()
        var seconds = ceil(remaining.inWholeMilliseconds / 1000.0).toInt()
        if (seconds <= 0) return
        _state.update { it.copy(cooldownSeconds = seconds) }
        countdownJob = viewModelScope.launch {
            while (seconds > 0) {
                delay(1.seconds)
                seconds--
                _state.update { it.copy(cooldownSeconds = seconds) }
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "email"
        const val KEY_SENT = "sent"
    }
}
