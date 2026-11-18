package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AppPreferencesRepository
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.AuthUser
import com.example.alpha_ai.core.domain.auth.LegacyCleanup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// :core:domain is a JVM module and cannot use the Android :core:testing fakes, so these minimal
// ones cover what the use cases call.

internal val USER = AuthUser(id = "uid", name = "Ali", email = "ali@example.com", emailConfirmed = true)

internal class StubAuthRepository(var user: AuthUser? = null) : AuthRepository {
    var refreshResult: AuthResult<AuthUser> = AuthResult.Failure(AuthFailure.Unknown)
    var signInResult: AuthResult<AuthUser> = AuthResult.Success(USER)
    var createResult: AuthResult<AuthUser> = AuthResult.Success(USER.copy(name = null))
    var saveProfileResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var confirmationResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var notice: String? = null
    var signOutCalls = 0
    val calls = mutableListOf<String>()

    override fun currentUser() = user

    override fun observeSignedIn(): Flow<Boolean> = MutableStateFlow(user != null)

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUser> {
        calls += "signIn($email)"
        return signInResult
    }

    override suspend fun createAccount(email: String, password: String, name: String): AuthResult<AuthUser> {
        calls += "createAccount($email,$name)"
        return createResult
    }

    override suspend fun saveProfile(name: String): AuthResult<Unit> {
        calls += "saveProfile($name)"
        return saveProfileResult
    }

    override suspend fun sendEmailConfirmation(): AuthResult<Unit> {
        calls += "sendEmailConfirmation"
        return confirmationResult
    }

    var resetResult: AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun sendPasswordReset(email: String): AuthResult<Unit> {
        calls += "sendPasswordReset($email)"
        return resetResult
    }

    override suspend fun refreshSession() = refreshResult

    override suspend fun signOut() {
        signOutCalls++
        user = null
    }

    override suspend fun consumeConfirmationNotice(): String? = notice.also { notice = null }
}

internal class StubPreferences(var introductionCompleted: Boolean = false) : AppPreferencesRepository {
    override suspend fun isIntroductionCompleted() = introductionCompleted

    override suspend fun setIntroductionCompleted() {
        introductionCompleted = true
    }
}

internal class CountingCleanup : LegacyCleanup {
    var runs = 0

    override suspend fun runOnce() {
        runs++
    }
}
