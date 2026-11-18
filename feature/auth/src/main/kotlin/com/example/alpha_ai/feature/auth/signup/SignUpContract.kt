package com.example.alpha_ai.feature.auth.signup

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField

/** contracts/auth-screens.md "Sign-up". */
internal data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val fieldErrors: Map<FormField, FieldError> = emptyMap(),
    val submitting: Boolean = false,
    /** True only while the account is being created; afterwards the sign-up finishes. */
    val cancellable: Boolean = false,
    val failure: AuthFailure? = null,
)

internal sealed interface SignUpIntent {
    data class NameChanged(val value: String) : SignUpIntent

    data class EmailChanged(val value: String) : SignUpIntent

    data class PasswordChanged(val value: String) : SignUpIntent

    data class ConfirmPasswordChanged(val value: String) : SignUpIntent

    data object TogglePasswordVisibility : SignUpIntent

    data object Submit : SignUpIntent

    data object Cancel : SignUpIntent

    data object SignInInstead : SignUpIntent
}

internal sealed interface SignUpEffect {
    data object Authenticated : SignUpEffect

    data object ToSignIn : SignUpEffect
}
