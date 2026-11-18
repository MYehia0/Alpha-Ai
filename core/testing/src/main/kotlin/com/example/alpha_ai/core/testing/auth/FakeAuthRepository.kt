package com.example.alpha_ai.core.testing.auth

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A scriptable [AuthRepository]. Set a result per operation before calling it; set a `gate` to
 * hold an operation in progress until the test completes it. Calls are recorded.
 */
class FakeAuthRepository(user: AuthUser? = null) : AuthRepository {

    var user: AuthUser? = user
        private set

    val signedIn = MutableStateFlow(user != null)

    var signInResult: AuthResult<AuthUser> = AuthResult.Success(TEST_USER)
    var createAccountResult: AuthResult<AuthUser> = AuthResult.Success(TEST_USER.copy(name = null))
    var saveProfileResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var emailConfirmationResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var passwordResetResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var refreshResult: AuthResult<AuthUser>? = null
    var confirmationNotice: String? = null

    /** When set, the next sign-in or sign-up waits for it to complete. */
    var gate: CompletableDeferred<Unit>? = null

    /** When set, saving the profile waits for it (the account already exists by then). */
    var profileGate: CompletableDeferred<Unit>? = null

    /** When set, sending a password reset waits for it. */
    var passwordResetGate: CompletableDeferred<Unit>? = null

    val signInCalls = mutableListOf<Pair<String, String>>()
    val createAccountCalls = mutableListOf<Triple<String, String, String>>()
    val saveProfileCalls = mutableListOf<String>()
    val passwordResetCalls = mutableListOf<String>()
    var emailConfirmationCalls = 0
    var refreshCalls = 0

    /** Calls that were cancelled while held by [gate]. */
    var cancelledCalls = 0
    var signOutCalls = 0

    override fun currentUser(): AuthUser? = user

    override fun observeSignedIn(): Flow<Boolean> = signedIn

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUser> {
        signInCalls += email to password
        awaitGate()
        return signInResult.also { if (it is AuthResult.Success) signInAs(it.value) }
    }

    override suspend fun createAccount(
        email: String,
        password: String,
        name: String,
    ): AuthResult<AuthUser> {
        createAccountCalls += Triple(email, password, name)
        awaitGate()
        return createAccountResult.also { if (it is AuthResult.Success) signInAs(it.value) }
    }

    override suspend fun saveProfile(name: String): AuthResult<Unit> {
        saveProfileCalls += name
        profileGate?.await()
        return saveProfileResult
    }

    override suspend fun sendEmailConfirmation(): AuthResult<Unit> {
        emailConfirmationCalls++
        return emailConfirmationResult
    }

    override suspend fun sendPasswordReset(email: String): AuthResult<Unit> {
        passwordResetCalls += email
        passwordResetGate?.await()
        return passwordResetResult
    }

    override suspend fun refreshSession(): AuthResult<AuthUser> {
        refreshCalls++
        return refreshResult ?: user?.let { AuthResult.Success(it) }
            ?: AuthResult.Failure(AuthFailure.SessionExpired)
    }

    override suspend fun signOut() {
        signOutCalls++
        signInAs(null)
    }

    override suspend fun consumeConfirmationNotice(): String? =
        confirmationNotice.also { confirmationNotice = null }

    private suspend fun awaitGate() {
        try {
            gate?.await()
        } catch (e: CancellationException) {
            cancelledCalls++
            throw e
        }
    }

    fun signInAs(value: AuthUser?) {
        user = value
        signedIn.value = value != null
    }

    companion object {
        val TEST_USER = AuthUser(
            id = "test-uid",
            name = "Test",
            email = "test@example.com",
            emailConfirmed = false,
        )
    }
}
