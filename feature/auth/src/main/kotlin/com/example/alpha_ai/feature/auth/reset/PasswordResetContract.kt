package com.example.alpha_ai.feature.auth.reset

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField

/** contracts/auth-screens.md "Password reset". */
internal data class PasswordResetUiState(
    val email: String = "",
    val fieldErrors: Map<FormField, FieldError> = emptyMap(),
    val submitting: Boolean = false,
    val cancellable: Boolean = false,
    val failure: AuthFailure? = null,
    /** A reset was requested; the same confirmation is shown for any email (FR-013). */
    val sent: Boolean = false,
    /** While above 0, Submit is disabled and reads "Send again in N s" (FR-014). */
    val cooldownSeconds: Int = 0,
) {
    val canSubmit: Boolean get() = !submitting && cooldownSeconds == 0
}

internal sealed interface PasswordResetIntent {
    data class EmailChanged(val value: String) : PasswordResetIntent

    data object Submit : PasswordResetIntent

    data object Cancel : PasswordResetIntent

    data object BackToSignIn : PasswordResetIntent
}

internal sealed interface PasswordResetEffect {
    data object ToSignIn : PasswordResetEffect
}
