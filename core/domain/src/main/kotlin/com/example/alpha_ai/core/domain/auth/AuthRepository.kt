package com.example.alpha_ai.core.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Accounts and the session on this phone. Every `suspend` function returns a result and never
 * throws, except `CancellationException` when the caller is cancelled. A request the caller
 * cancelled that the service completes anyway leaves the user signed out (data-model.md
 * "Cancellation").
 */
interface AuthRepository {
    /** The signed-in user from local state only; no network. */
    fun currentUser(): AuthUser?

    /** Emits whether a user is signed in, now and whenever it changes. */
    fun observeSignedIn(): Flow<Boolean>

    suspend fun signIn(email: String, password: String): AuthResult<AuthUser>

    /** Creates the account and signs it in. [name] is used only for cancellation clean-up. */
    suspend fun createAccount(email: String, password: String, name: String): AuthResult<AuthUser>

    /** Saves the name for the signed-in user. A failure is remembered and retried later. */
    suspend fun saveProfile(name: String): AuthResult<Unit>

    suspend fun sendEmailConfirmation(): AuthResult<Unit>

    /** An email with no account is reported as success (FR-013). */
    suspend fun sendPasswordReset(email: String): AuthResult<Unit>

    /** Checks the session with the service and refreshes the cached profile. */
    suspend fun refreshSession(): AuthResult<AuthUser>

    /** Local sign-out; works offline and never fails. */
    suspend fun signOut()

    /** The email for the one-time "confirmation link sent" message, once; then `null`. */
    suspend fun consumeConfirmationNotice(): String?
}
