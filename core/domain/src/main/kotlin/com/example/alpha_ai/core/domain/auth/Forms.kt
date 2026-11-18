package com.example.alpha_ai.core.domain.auth

import java.util.Locale

/** Email normalization and a basic format check. The service performs the final check. */
object Email {
    /** Leading and trailing spaces removed, lower case (FR-020). */
    fun normalize(raw: String): String = raw.trim().lowercase(Locale.ROOT)

    /**
     * True for `local@domain.tld`: exactly one `@`, a non-empty local part, a domain with at
     * least one dot (not at either end), and no spaces.
     */
    fun isValid(email: String): Boolean {
        if (email.isEmpty() || email.any { it.isWhitespace() }) return false
        val parts = email.split('@')
        if (parts.size != 2) return false
        val (local, domain) = parts
        if (local.isEmpty()) return false
        val dot = domain.indexOf('.')
        return dot > 0 && !domain.endsWith('.') && !domain.contains("..")
    }
}

/** The fields of the account forms. */
enum class FormField { Name, Email, Password, ConfirmPassword }

/** What is wrong with one field. */
enum class FieldError { Empty, InvalidFormat, TooShort, Mismatch }

/** The shortest password the app accepts, counted in Unicode code points. */
const val MIN_PASSWORD_LENGTH = 8

private fun String.codePointLength(): Int = codePointCount(0, length)

private fun MutableMap<FormField, FieldError>.checkEmail(raw: String) {
    val email = Email.normalize(raw)
    when {
        email.isEmpty() -> put(FormField.Email, FieldError.Empty)
        !Email.isValid(email) -> put(FormField.Email, FieldError.InvalidFormat)
    }
}

/** Sign-up form (FR-006, FR-007). [validate] returns an empty map when the form can be sent. */
data class SignUpForm(
    val name: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
) {
    fun validate(): Map<FormField, FieldError> = buildMap {
        if (name.isBlank()) put(FormField.Name, FieldError.Empty)
        checkEmail(email)
        when {
            password.isEmpty() -> put(FormField.Password, FieldError.Empty)
            password.codePointLength() < MIN_PASSWORD_LENGTH ->
                put(FormField.Password, FieldError.TooShort)
        }
        if (confirmPassword != password) put(FormField.ConfirmPassword, FieldError.Mismatch)
    }
}

/** Sign-in form (FR-009). */
data class SignInForm(val email: String, val password: String) {
    fun validate(): Map<FormField, FieldError> = buildMap {
        checkEmail(email)
        if (password.isEmpty()) put(FormField.Password, FieldError.Empty)
    }
}

/** Password reset form (FR-012). */
data class PasswordResetForm(val email: String) {
    fun validate(): Map<FormField, FieldError> = buildMap { checkEmail(email) }
}
