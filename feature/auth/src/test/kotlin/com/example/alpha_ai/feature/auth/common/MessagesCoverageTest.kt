package com.example.alpha_ai.feature.auth.common

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.SignInReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MessagesCoverageTest {

    // The exhaustive `when` in AuthFailureMessages already fails the build on a new case.
    private val allFailures: List<AuthFailure> = listOf(
        AuthFailure.NoConnection,
        AuthFailure.InvalidCredentials,
        AuthFailure.EmailAlreadyInUse,
        AuthFailure.WeakPassword,
        AuthFailure.InvalidEmail,
        AuthFailure.TooManyAttempts,
        AuthFailure.AccountDisabled,
        AuthFailure.SessionExpired,
        AuthFailure.Unknown,
    )

    @Test
    fun `every failure has its own message`() {
        assertEquals(9, allFailures.size)
        val messages = allFailures.map { AuthFailureMessages.messageRes(it) }
        messages.forEach { assertNotEquals(0, it) }
        assertEquals(allFailures.size, messages.toSet().size)
    }

    @Test
    fun `every field error has a message for every field`() {
        FormField.entries.forEach { field ->
            FieldError.entries.forEach { error ->
                assertNotEquals("$field/$error", 0, FieldErrorMessages.messageRes(field, error))
            }
        }
    }

    @Test
    fun `every sign-in reason has a notice`() {
        SignInReason.entries.forEach { assertNotEquals(0, AuthFailureMessages.noticeRes(it)) }
    }
}
