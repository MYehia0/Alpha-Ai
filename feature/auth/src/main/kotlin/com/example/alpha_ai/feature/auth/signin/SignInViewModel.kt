package com.example.alpha_ai.feature.auth.signin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.core.domain.auth.usecase.FormSubmission
import com.example.alpha_ai.core.domain.auth.usecase.SignInUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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

/**
 * Sign-in (contracts/auth-screens.md). [reason] is the route argument; its notice is shown until
 * dismissed, also across rotation. The email survives process death; the password never does.
 */
@HiltViewModel(assistedFactory = SignInViewModel.Factory::class)
internal class SignInViewModel @AssistedInject constructor(
    @Assisted reason: SignInReason?,
    private val savedStateHandle: SavedStateHandle,
    private val signIn: SignInUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(reason: SignInReason?): SignInViewModel
    }

    private val _state = MutableStateFlow(
        SignInUiState(
            email = savedStateHandle[KEY_EMAIL] ?: "",
            notice = reason.takeUnless { savedStateHandle[KEY_NOTICE_DISMISSED] ?: false },
        ),
    )
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _effects = Channel<SignInEffect>(Channel.BUFFERED)
    val effects: Flow<SignInEffect> = _effects.receiveAsFlow()

    private var submitJob: Job? = null

    fun onIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EmailChanged -> {
                savedStateHandle[KEY_EMAIL] = intent.value
                edit(FormField.Email) { it.copy(email = intent.value) }
            }
            is SignInIntent.PasswordChanged -> edit(FormField.Password) { it.copy(password = intent.value) }
            SignInIntent.TogglePasswordVisibility -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            SignInIntent.Submit -> submit()
            SignInIntent.Cancel -> cancel()
            SignInIntent.ForgotPassword -> _effects.trySend(SignInEffect.ToPasswordReset(_state.value.email.trim()))
            SignInIntent.CreateAccount -> _effects.trySend(SignInEffect.ToSignUp)
            SignInIntent.DismissNotice -> {
                savedStateHandle[KEY_NOTICE_DISMISSED] = true
                _state.update { it.copy(notice = null) }
            }
        }
    }

    private fun edit(field: FormField, change: (SignInUiState) -> SignInUiState) {
        _state.update { change(it).copy(fieldErrors = it.fieldErrors - field, failure = null) }
    }

    private fun submit() {
        val current = _state.value
        if (current.submitting) return
        // The previous failure stays until the result arrives, so the layout does not shift under a
        // second tap and put Cancel where Submit was.
        _state.update { it.copy(submitting = true, cancellable = true) }
        submitJob = viewModelScope.launch {
            when (val result = signIn(current.email, current.password)) {
                is FormSubmission.FieldErrors ->
                    _state.update {
                        it.copy(submitting = false, cancellable = false, fieldErrors = result.errors, failure = null)
                    }
                is FormSubmission.Submitted -> when (val outcome = result.result) {
                    is AuthResult.Failure ->
                        _state.update { it.copy(submitting = false, cancellable = false, failure = outcome.failure) }
                    is AuthResult.Success -> _effects.send(SignInEffect.Authenticated)
                }
            }
        }
    }

    /** Stops the request and keeps the fields; the repository signs out a late success. */
    private fun cancel() {
        if (!_state.value.cancellable) return
        submitJob?.cancel()
        submitJob = null
        _state.update { it.copy(submitting = false, cancellable = false, failure = null) }
    }

    private companion object {
        const val KEY_EMAIL = "email"
        const val KEY_NOTICE_DISMISSED = "notice_dismissed"
    }
}
