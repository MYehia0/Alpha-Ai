package com.example.alpha_ai.core.data.auth

import com.example.alpha_ai.core.data.auth.AuthErrorSignal.Kind
import com.example.alpha_ai.core.domain.auth.AuthFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirebaseErrorMapperTest {

    private val mapper = FirebaseErrorMapper()

    private fun code(code: String, operation: AuthOperation = AuthOperation.SignIn) =
        mapper.map(AuthErrorSignal(Kind.Auth, code), operation)

    @Test
    fun `transport kinds`() {
        assertEquals(AuthFailure.NoConnection, mapper.map(AuthErrorSignal(Kind.Network), AuthOperation.SignIn))
        assertEquals(
            AuthFailure.TooManyAttempts,
            mapper.map(AuthErrorSignal(Kind.TooManyRequests), AuthOperation.SignIn),
        )
        assertEquals(AuthFailure.Unknown, mapper.map(AuthErrorSignal(Kind.Other), AuthOperation.SignIn))
    }

    @Test
    fun `every error code in the table`() {
        assertEquals(AuthFailure.NoConnection, code("ERROR_NETWORK_REQUEST_FAILED"))
        assertEquals(AuthFailure.InvalidCredentials, code("ERROR_INVALID_CREDENTIAL"))
        assertEquals(AuthFailure.InvalidCredentials, code("ERROR_WRONG_PASSWORD"))
        assertEquals(AuthFailure.InvalidEmail, code("ERROR_INVALID_EMAIL"))
        assertEquals(AuthFailure.EmailAlreadyInUse, code("ERROR_EMAIL_ALREADY_IN_USE", AuthOperation.CreateAccount))
        assertEquals(AuthFailure.WeakPassword, code("ERROR_WEAK_PASSWORD", AuthOperation.CreateAccount))
        assertEquals(AuthFailure.TooManyAttempts, code("ERROR_TOO_MANY_REQUESTS"))
        assertEquals(AuthFailure.AccountDisabled, code("ERROR_USER_DISABLED"))
        assertEquals(AuthFailure.SessionExpired, code("ERROR_USER_TOKEN_EXPIRED", AuthOperation.Refresh))
        assertEquals(AuthFailure.SessionExpired, code("ERROR_INVALID_USER_TOKEN", AuthOperation.Refresh))
        assertEquals(AuthFailure.Unknown, code("ERROR_SOMETHING_NEW"))
    }

    @Test
    fun `user not found depends on the operation`() {
        assertEquals(AuthFailure.InvalidCredentials, code("ERROR_USER_NOT_FOUND", AuthOperation.SignIn))
        assertEquals(AuthFailure.SessionExpired, code("ERROR_USER_NOT_FOUND", AuthOperation.Refresh))
        assertNull(code("ERROR_USER_NOT_FOUND", AuthOperation.PasswordReset))
    }

    @Test
    fun `a missing code is unknown`() {
        assertEquals(AuthFailure.Unknown, mapper.map(AuthErrorSignal(Kind.Auth, null), AuthOperation.SignIn))
    }
}
