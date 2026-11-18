package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.SignInReason
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshSessionUseCaseTest {

    private val repository = StubAuthRepository(USER)
    private val refresh = RefreshSessionUseCase(repository)

    private suspend fun outcomeFor(failure: AuthFailure): RefreshOutcome {
        repository.refreshResult = AuthResult.Failure(failure)
        return refresh()
    }

    @Test
    fun `a valid session stays signed in`() = runTest {
        repository.refreshResult = AuthResult.Success(USER)
        assertEquals(RefreshOutcome.StillSignedIn(), refresh())
        assertEquals(0, repository.signOutCalls)
    }

    @Test
    fun `an expired session signs out`() = runTest {
        assertEquals(RefreshOutcome.SignedOut(SignInReason.SessionExpired), outcomeFor(AuthFailure.SessionExpired))
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun `a disabled account signs out`() = runTest {
        assertEquals(RefreshOutcome.SignedOut(SignInReason.AccountDisabled), outcomeFor(AuthFailure.AccountDisabled))
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun `offline and unknown stay signed in and report the failure`() = runTest {
        assertEquals(
            RefreshOutcome.StillSignedIn(AuthFailure.NoConnection),
            outcomeFor(AuthFailure.NoConnection),
        )
        assertEquals(RefreshOutcome.StillSignedIn(AuthFailure.Unknown), outcomeFor(AuthFailure.Unknown))
        assertEquals(0, repository.signOutCalls)
    }
}
