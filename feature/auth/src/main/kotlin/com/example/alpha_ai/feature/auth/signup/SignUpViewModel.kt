package com.example.alpha_ai.feature.auth.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.SignUpForm
import com.example.alpha_ai.core.domain.auth.usecase.FormSubmission
import com.example.alpha_ai.core.domain.auth.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Sign-up (contracts/auth-screens.md). Name and email survive process death; the passwords are
 * kept in memory only (FR-022).
 */
@HiltViewModel
internal class SignUpViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val signUp: SignUpUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SignUpUiState(
            name = savedStateHandle[KEY_NAME] ?: "",
            email = savedStateHandle[KEY_EMAIL] ?: "",
        ),
    )
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _effects = Channel<SignUpEffect>(Channel.BUFFERED)
    val effects: Flow<SignUpEffect> = _effects.receiveAsFlow()

    private var submitJob: Job? = null

    fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.NameChanged -> {
                savedStateHandle[KEY_NAME] = intent.value
                edit(FormField.Name) { it.copy(name = intent.value) }
            }
            is SignUpIntent.EmailChanged -> {
                savedStateHandle[KEY_EMAIL] = intent.value
                edit(FormField.Email) { it.copy(email = intent.value) }
            }
            is SignUpIntent.PasswordChanged -> edit(FormField.Password) { it.copy(password = intent.value) }
            is SignUpIntent.ConfirmPasswordChanged ->
                edit(FormField.ConfirmPassword) { it.copy(confirmPassword = intent.value) }
            SignUpIntent.TogglePasswordVisibility -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            SignUpIntent.Submit -> submit()
            SignUpIntent.Cancel -> cancel()
            SignUpIntent.SignInInstead -> _effects.trySend(SignUpEffect.ToSignIn)
        }
    }

    /** Editing a field clears its error and the failure; typed text is never cleared (FR-019). */
    private fun edit(field: FormField, change: (SignUpUiState) -> SignUpUiState) {
        _state.update { change(it).copy(fieldErrors = it.fieldErrors - field, failure = null) }
    }

    private fun submit() {
        val current = _state.value
        if (current.submitting) return
        // The previous failure stays until the result arrives, so the layout does not shift under a
        // second tap and put Cancel where Submit was.
        _state.update { it.copy(submitting = true, cancellable = true) }
        submitJob = viewModelScope.launch {
            val form = SignUpForm(current.name, current.email, current.password, current.confirmPassword)
            val result = signUp(form, onAccountCreated = { _state.update { it.copy(cancellable = false) } })
            when (result) {
                is FormSubmission.FieldErrors ->
                    _state.update {
                        it.copy(submitting = false, cancellable = false, fieldErrors = result.errors, failure = null)
                    }
                is FormSubmission.Submitted -> when (val outcome = result.result) {
                    is AuthResult.Failure ->
                        _state.update { it.copy(submitting = false, cancellable = false, failure = outcome.failure) }
                    is AuthResult.Success -> {
                        // Not shown to the user (research §8), so logged by case name only.
                        outcome.value.unshownFailures.forEach { Timber.i("Sign-up step not completed: %s", it) }
                        _effects.send(SignUpEffect.Authenticated)
                    }
                }
            }
        }
    }

    /** Stops the request and keeps the form; the repository signs out a late success. */
    private fun cancel() {
        if (!_state.value.cancellable) return
        submitJob?.cancel()
        submitJob = null
        _state.update { it.copy(submitting = false, cancellable = false, failure = null) }
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
    }
}
