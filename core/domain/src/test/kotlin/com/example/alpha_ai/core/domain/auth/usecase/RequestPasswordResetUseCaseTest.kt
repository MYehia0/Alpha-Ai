package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.ActionCooldown
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class RequestPasswordResetUseCaseTest {

    private val time = TestTimeSource()
    private val repository = StubAuthRepository()
    private val reset = RequestPasswordResetUseCase(repository, ActionCooldown(time))

    @Test
    fun `sends to the normalized email`() = runTest {
        assertEquals(PasswordResetResult.Sent, reset(" Ali@Example.com "))
        assertEquals(listOf("sendPasswordReset(ali@example.com)"), repository.calls)
    }

    @Test
    fun `invalid email sends nothing`() = runTest {
        assertEquals(PasswordResetResult.FieldErrors(mapOf(FormField.Email to FieldError.InvalidFormat)), reset("ali@"))
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `cooling down at 30 s, free at 60 s`() = runTest {
        reset("ali@example.com")
        time += 30.seconds
        assertEquals(PasswordResetResult.CoolingDown(30.seconds), reset("ali@example.com"))
        assertEquals(1, repository.calls.size)

        time += 30.seconds
        assertEquals(Duration.ZERO, reset.remainingCooldown())
        assertEquals(PasswordResetResult.Sent, reset("ali@example.com"))
    }

    @Test
    fun `a failure does not start the cooldown`() = runTest {
        repository.resetResult = AuthResult.Failure(AuthFailure.NoConnection)
        assertEquals(PasswordResetResult.Failure(AuthFailure.NoConnection), reset("ali@example.com"))
        assertEquals(Duration.ZERO, reset.remainingCooldown())
    }
}
