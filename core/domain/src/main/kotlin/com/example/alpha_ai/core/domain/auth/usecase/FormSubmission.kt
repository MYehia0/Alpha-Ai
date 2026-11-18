package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField

/** The result of submitting a form: rejected locally, or sent to the service. */
sealed interface FormSubmission<out T> {
    /** Nothing was sent (FR-007). */
    data class FieldErrors(val errors: Map<FormField, FieldError>) : FormSubmission<Nothing>

    data class Submitted<out T>(val result: AuthResult<T>) : FormSubmission<T>
}
