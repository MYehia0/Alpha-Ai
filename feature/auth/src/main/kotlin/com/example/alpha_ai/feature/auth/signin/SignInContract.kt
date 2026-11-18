package com.example.alpha_ai.feature.auth.signin

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.SignInReason

/** contracts/auth-screens.md "Sign-in". */
internal data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val fieldErrors: Map<FormField, FieldError> = emptyMap(),
    val submitting: Boolean = false,
    val cancellable: Boolean = false,
    val failure: AuthFailure? = null,
    /** Why the user was sent here (signed out, session ended, account disabled), shown once. */
    val notice: SignInReason? = null,
)

internal sealed interface SignInIntent {
    data class EmailChanged(val value: String) : SignInIntent

    data class PasswordChanged(val value: String) : SignInIntent

    data object TogglePasswordVisibility : SignInIntent

    data object Submit : SignInIntent

    data object Cancel : SignInIntent

    data object ForgotPassword : SignInIntent

    data object CreateAccount : SignInIntent

    data object DismissNotice : SignInIntent
}

internal sealed interface SignInEffect {
    data object Authenticated : SignInEffect

    data class ToPasswordReset(val email: String) : SignInEffect

    data object ToSignUp : SignInEffect
}
