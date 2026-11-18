package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.SignInReason
import javax.inject.Inject

/** What checking the session with the service decided. */
sealed interface RefreshOutcome {
    /**
     * The user stays signed in. [unshownFailure] is set when the check itself failed (offline,
     * or an unexpected error): the spec does not show it, so the caller must log its case name
     * (constitution VIII).
     */
    data class StillSignedIn(val unshownFailure: AuthFailure? = null) : RefreshOutcome

    /** The session is no longer valid; the user has been signed out for [reason]. */
    data class SignedOut(val reason: SignInReason) : RefreshOutcome
}

/** Checks the session when home opens; an invalid one signs the user out (FR-018). */
class RefreshSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): RefreshOutcome =
        when (val result = authRepository.refreshSession()) {
            is AuthResult.Success -> RefreshOutcome.StillSignedIn()
            is AuthResult.Failure -> when (result.failure) {
                AuthFailure.SessionExpired -> signOut(SignInReason.SessionExpired)
                AuthFailure.AccountDisabled -> signOut(SignInReason.AccountDisabled)
                // Only a definite answer from the service ends a session: being offline, being
                // throttled, or an unknown error keeps the user in (FR-002).
                else -> RefreshOutcome.StillSignedIn(unshownFailure = result.failure)
            }
        }

    private suspend fun signOut(reason: SignInReason): RefreshOutcome {
        authRepository.signOut()
        return RefreshOutcome.SignedOut(reason)
    }
}
