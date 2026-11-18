package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInUseCaseTest {

    private val repository = StubAuthRepository()
    private val signIn = SignInUseCase(repository)

    @Test
    fun `email is normalized`() = runTest {
        signIn("  Ali@Example.COM ", "secret12")
        assertEquals(listOf("signIn(ali@example.com)"), repository.calls)
    }

    @Test
    fun `field errors stop before the call`() = runTest {
        assertEquals(
            FormSubmission.FieldErrors(mapOf(FormField.Email to FieldError.Empty, FormField.Password to FieldError.Empty)),
            signIn(" ", ""),
        )
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `failures pass through`() = runTest {
        repository.signInResult = AuthResult.Failure(AuthFailure.InvalidCredentials)
        assertEquals(FormSubmission.Submitted(AuthResult.Failure(AuthFailure.InvalidCredentials)), signIn("a@b.co", "x"))
        repository.signInResult = AuthResult.Failure(AuthFailure.NoConnection)
        assertEquals(FormSubmission.Submitted(AuthResult.Failure(AuthFailure.NoConnection)), signIn("a@b.co", "x"))
    }

    @Test
    fun `success returns the user`() = runTest {
        assertEquals(FormSubmission.Submitted(AuthResult.Success(USER)), signIn("ali@example.com", "secret12"))
    }
}
