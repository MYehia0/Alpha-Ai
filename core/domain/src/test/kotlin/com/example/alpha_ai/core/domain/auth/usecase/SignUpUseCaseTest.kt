package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.SignUpForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpUseCaseTest {

    private val repository = StubAuthRepository()
    private val signUp = SignUpUseCase(repository)
    private val form = SignUpForm(" Ali ", "  Ali@Example.COM ", "secret12", "secret12")

    private fun FormSubmission<SignUpSuccess>.success() =
        ((this as FormSubmission.Submitted).result as AuthResult.Success).value

    @Test
    fun `email is normalized and name trimmed before any call`() = runTest {
        signUp(form)
        assertEquals(
            listOf("createAccount(ali@example.com,Ali)", "saveProfile(Ali)", "sendEmailConfirmation"),
            repository.calls,
        )
    }

    @Test
    fun `field errors stop before any call`() = runTest {
        val result = signUp(form.copy(confirmPassword = "other"))
        assertEquals(FormSubmission.FieldErrors(mapOf(FormField.ConfirmPassword to FieldError.Mismatch)), result)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `email already in use passes through`() = runTest {
        repository.createResult = AuthResult.Failure(AuthFailure.EmailAlreadyInUse)
        assertEquals(FormSubmission.Submitted(AuthResult.Failure(AuthFailure.EmailAlreadyInUse)), signUp(form))
        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `profile and confirmation failures still succeed and are reported`() = runTest {
        repository.saveProfileResult = AuthResult.Failure(AuthFailure.NoConnection)
        repository.confirmationResult = AuthResult.Failure(AuthFailure.TooManyAttempts)
        val success = signUp(form).success()
        assertEquals("Ali", success.user.name)
        assertEquals(listOf(AuthFailure.NoConnection, AuthFailure.TooManyAttempts), success.unshownFailures)
    }

    @Test
    fun `account-created callback fires only after the account exists`() = runTest {
        var created = 0
        signUp(form) { created++ }
        assertEquals(1, created)
        repository.createResult = AuthResult.Failure(AuthFailure.WeakPassword)
        signUp(form) { created++ }
        assertEquals(1, created)
    }
}
