package com.example.alpha_ai.feature.auth.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.MIN_PASSWORD_LENGTH
import com.example.alpha_ai.feature.auth.R

/** The message under a field for each [FieldError]. */
object FieldErrorMessages {

    @StringRes
    fun messageRes(field: FormField, error: FieldError): Int = when (error) {
        FieldError.Empty -> when (field) {
            FormField.Name -> R.string.auth_field_name_empty
            FormField.Email -> R.string.auth_field_email_empty
            FormField.Password, FormField.ConfirmPassword -> R.string.auth_field_password_empty
        }
        FieldError.InvalidFormat -> R.string.auth_field_email_invalid
        FieldError.TooShort -> R.string.auth_field_password_too_short
        FieldError.Mismatch -> R.string.auth_field_confirm_mismatch
    }

    /** The resolved message for [field] in [errors], or `null` when the field has no error. */
    @Composable
    fun message(errors: Map<FormField, FieldError>, field: FormField): String? {
        val error = errors[field] ?: return null
        return stringResource(messageRes(field, error), MIN_PASSWORD_LENGTH)
    }
}
