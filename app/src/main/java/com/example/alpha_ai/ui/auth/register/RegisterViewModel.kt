package com.example.alpha_ai.ui.auth.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.alpha_ai.core.base.BaseViewModel
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.core.common.UiEvent
import com.example.alpha_ai.data.repository.AuthRepository
import com.example.alpha_ai.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    val fullName = MutableLiveData("")
    val email = MutableLiveData("")
    val password = MutableLiveData("")
    val passwordConfirm = MutableLiveData("")

    val fullNameError = MutableLiveData<String?>()
    val emailError = MutableLiveData<String?>()
    val passwordError = MutableLiveData<String?>()
    val passwordConfirmError = MutableLiveData<String?>()

    private val _authState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val authState = _authState.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid = _isFormValid.asStateFlow()

    init {
        setupReactiveValidation()
    }

    private fun setupReactiveValidation() {
        viewModelScope.launch {
            combine(
                fullName.asFlow(),
                email.asFlow(),
                password.asFlow(),
                passwordConfirm.asFlow()
            ) { name, email, password, confirm ->
                validateFormReactive(name, email, password, confirm)
                validateFullName(false)
                validateEmail(false)
                validatePassword(false)
                validatePasswordConfirm(false)
            }.collect { isValid ->
                _isFormValid.value = isValid
            }
        }
    }

    private fun validateFormReactive(
        name: String?,
        email: String?,
        password: String?,
        confirm: String?
    ): Boolean {
        return !name.isNullOrBlank() &&
                name.trim().length >= 2 &&
                !email.isNullOrBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                !password.isNullOrBlank() &&
                password.length >= 6 &&
                !confirm.isNullOrBlank() &&
                confirm == password
    }

    fun onRegisterClick() {
        val isFullNameValid = validateFullName(showRequiredError = true)
        val isEmailValid = validateEmail(showRequiredError = true)
        val isPasswordValid = validatePassword(showRequiredError = true)
        val isPasswordConfirmValid = validatePasswordConfirm(showRequiredError = true)

        if (!isFullNameValid || !isEmailValid || !isPasswordValid || !isPasswordConfirmValid) {
            return
        }

        performRegistration()
    }

    private fun performRegistration() {
        viewModelScope.launch {
            _authState.value = UiState.Loading

            authRepository.register(
                email = email.value!!,
                password = password.value!!,
                fullName = fullName.value!!
            ).fold(
                onSuccess = { user ->
                    _authState.value = UiState.Success(user.uid ?: "")
                    sendEvent(UiEvent.ShowSnackbar("Registration successful! Please login."))
                    sendEvent(UiEvent.Navigate(NavigationDestination.Login))
                },
                onFailure = { exception ->
                    val errorMessage = mapErrorMessage(exception)
                    _authState.value = UiState.Error(errorMessage, exception)
                    sendEvent(UiEvent.ShowSnackbar(errorMessage))
                }
            )
        }
    }

    fun onLoginClick() {
        sendEvent(UiEvent.Navigate(NavigationDestination.Login))
    }

    fun onBackClick() {
        sendEvent(UiEvent.Navigate(NavigationDestination.Back))
    }

    private fun validateFullName(showRequiredError: Boolean): Boolean {
        fullNameError.value = when {
            fullName.value.isNullOrBlank() -> {
                if (showRequiredError) "Full name is required" else null
            }
            fullName.value!!.trim().length < 2 -> {
                "Name must be at least 2 characters"
            }
            else -> null
        }
        return fullNameError.value == null
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
            password.value!!.length < 6 -> {
                "Password must be at least 6 characters"
            }
            else -> null
        }
        return passwordError.value == null
    }

    private fun validatePasswordConfirm(showRequiredError: Boolean): Boolean {
        passwordConfirmError.value = when {
            passwordConfirm.value.isNullOrBlank() -> {
                if (showRequiredError) "Confirm password is required" else null
            }
            passwordConfirm.value != password.value -> {
                "Passwords don't match"
            }
            else -> null
        }
        return passwordConfirmError.value == null
    }

    private fun mapErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("email address is already in use", ignoreCase = true) == true ||
                    exception.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true ->
                "This email is already registered"

            exception.message?.contains("email address is badly formatted", ignoreCase = true) == true ||
                    exception.message?.contains("INVALID_EMAIL", ignoreCase = true) == true ->
                "Invalid email format"

            exception.message?.contains("password", ignoreCase = true) == true &&
                    exception.message?.contains("weak", ignoreCase = true) == true ->
                "Password is too weak"

            exception.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection"

            else -> exception.message ?: "Registration failed. Please try again"
        }
    }

    fun clearError() {
        if (_authState.value is UiState.Error) {
            _authState.value = UiState.Idle
        }
    }
}