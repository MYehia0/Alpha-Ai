package com.example.alpha_ai.ui.auth.login

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.base.BaseViewModel
import com.example.alpha_ai.core.common.DialogAction
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.core.common.SnackbarAction
import com.example.alpha_ai.core.common.UiEvent
import com.example.alpha_ai.core.common.UiState
import com.example.alpha_ai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    val email = MutableLiveData("")
    val password = MutableLiveData("")
    val rememberMe = MutableLiveData(false)

    val emailError = MutableLiveData<String?>()
    val passwordError = MutableLiveData<String?>()

    private val _authState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val authState = _authState.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid = _isFormValid.asStateFlow()

    init {
        loadSavedCredentials()
        setupReactiveValidation()
    }

    private fun loadSavedCredentials() {
        if (authRepository.isRememberMeEnabled()) {
            authRepository.getSavedCredentials()?.let { (savedEmail, savedPassword) ->
                email.value = savedEmail
                password.value = savedPassword
                rememberMe.value = true
            }
        }
    }

    private fun setupReactiveValidation() {
        viewModelScope.launch {
            combine(
                email.asFlow(),
                password.asFlow(),
            ) { email, password ->
                validateFormReactive(email, password)
                validateEmail(false)
                validatePassword(false)
            }.collect { isValid ->
                _isFormValid.value = isValid
            }
        }
    }

    private fun validateFormReactive(
        email: String?,
        password: String?,
    ): Boolean {
        return  !email.isNullOrBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                !password.isNullOrBlank() &&
                password.length >= 6
    }

    fun onLogInClick() {
        val isEmailValid = validateEmail(showRequiredError = true)
        val isPasswordValid = validatePassword(showRequiredError = true)

        if (!isEmailValid || !isPasswordValid) {
            return
        }

        performLogin()
    }

    private fun performLogin() {
        viewModelScope.launch {
            _authState.value = UiState.Loading

            authRepository.signIn(
                email = email.value!!,
                password = password.value!!,
                rememberMe = rememberMe.value ?: false
            ).fold(
                onSuccess = { user ->
                    _authState.value = UiState.Success(user.uid ?: "")
                    Log.e("LoginViewModel", "performLogin: User UID: ${user.uid}" )
                    sendEvent(UiEvent.ShowSnackbar("Welcome back!"))
                    sendEvent(UiEvent.Navigate(NavigationDestination.Home))
                },
                onFailure = { exception ->
                    val errorMessage = mapErrorMessage(exception)
                    _authState.value = UiState.Error(errorMessage, exception)
                    Log.e("LoginViewModel", "performLogin: ${errorMessage}" )
                    Log.e("LoginViewModel", "performLogin: ${exception}" )
                    sendEvent(
                        UiEvent.ShowSnackbar(
                            message = errorMessage,
                            action = SnackbarAction("RETRY") { performLogin() }
                        )
                    )
                }
            )
        }
    }

    fun onBackClick() {
        sendEvent(UiEvent.Navigate(NavigationDestination.Back))
    }

    fun onForgotPasswordClick() {
        sendEvent(
            UiEvent.ShowDialog(
                title = "Coming Soon",
                message = "Forgot Password will be available in the next update.",
                positiveButton = DialogAction("OK") { }
            )
        )
    }

    fun onGoogleSignInClick() {
        sendEvent(
            UiEvent.ShowDialog(
                title = "Coming Soon",
                message = "Google Sign-In will be available in the next update.",
                positiveButton = DialogAction("OK") { }
            )
        )
    }

    fun onFacebookSignInClick() {
        sendEvent(
            UiEvent.ShowDialog(
                title = "Coming Soon",
                message = "Facebook Sign-In will be available in the next update.",
                positiveButton = DialogAction("OK") { }
            )
        )
    }

    fun onRegisterClick() {
        sendEvent(UiEvent.Navigate(NavigationDestination.Register))
    }

    private fun validateEmail(showRequiredError: Boolean): Boolean {
        emailError.value = when {
            email.value.isNullOrBlank() -> {
                if (showRequiredError) "Email is required" else null
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.value!!).matches() -> {
                "Invalid email format"
            }
            else -> null
        }
        return emailError.value == null
    }

    private fun validatePassword(showRequiredError: Boolean): Boolean {
        passwordError.value = when {
            password.value.isNullOrBlank() -> {
                if (showRequiredError) "Password is required" else null
            }
            password.value?.length!! < 6 -> {
                "Password must be at least 6 characters"
            }
            else -> null
        }
        return passwordError.value == null
    }

    private fun mapErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("password is invalid", ignoreCase = true) == true ||
                    exception.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                "Invalid email or password"

            exception.message?.contains("no user record", ignoreCase = true) == true ||
                    exception.message?.contains("USER_NOT_FOUND", ignoreCase = true) == true ->
                "No account found with this email"

            exception.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection"

            exception.message?.contains("too-many-requests", ignoreCase = true) == true ->
                "Too many attempts. Please try again later"

            else -> exception.message ?: "Login failed. Please try again"
        }
    }

    fun clearError() {
        if (_authState.value is UiState.Error) {
            _authState.value = UiState.Idle
        }
    }
}