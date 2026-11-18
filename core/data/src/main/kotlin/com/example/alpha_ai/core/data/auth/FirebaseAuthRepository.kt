package com.example.alpha_ai.core.data.auth

import com.example.alpha_ai.core.common.ApplicationScope
import com.example.alpha_ai.core.common.IoDispatcher
import com.example.alpha_ai.core.datastore.SessionData
import com.example.alpha_ai.core.datastore.SessionStore
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.AuthUser
import com.example.alpha_ai.core.domain.auth.Email
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AuthRepository] on Firebase Auth, the `users/{uid}` profile document, and the encrypted
 * [SessionStore]. Firebase's own session is the source of truth for "signed in"; the store only
 * caches the profile and two flags. Nothing here logs user data (FR-024).
 */
@Singleton
internal class FirebaseAuthRepository @Inject constructor(
    private val source: FirebaseAuthDataSource,
    private val session: SessionStore,
    private val mapper: FirebaseErrorMapper,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : AuthRepository {

    /** The latest cache, so [currentUser] can answer without suspending. */
    private val cache: StateFlow<SessionData> =
        session.data.stateIn(appScope, SharingStarted.Eagerly, SessionData.EMPTY)

    override fun currentUser(): AuthUser? {
        val account = source.currentAccount() ?: return null
        val cached = cache.value.takeIf { it.userId == account.uid }
        return AuthUser(
            id = account.uid,
            name = cached?.name,
            email = Email.normalize(account.email ?: cached?.email.orEmpty()),
            emailConfirmed = account.emailVerified,
        )
    }

    override fun observeSignedIn(): Flow<Boolean> = source.authStateChanges()

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUser> =
        withContext(io) {
            attempt(AuthOperation.SignIn) {
                val account = source.signIn(email, password).awaitOrCleanUp { signOut() }
                val name = readName(account.uid)
                val user = account.toUser(name, fallbackEmail = email)
                session.update {
                    SessionData(
                        userId = account.uid,
                        name = name,
                        email = user.email,
                        emailConfirmed = account.emailVerified,
                    )
                }
                user
            }
        }

    override suspend fun createAccount(
        email: String,
        password: String,
        name: String,
    ): AuthResult<AuthUser> = withContext(io) {
        attempt(AuthOperation.CreateAccount) {
            val account = source.createUser(email, password).awaitOrCleanUp { created ->
                // Cancelled, but the account exists: keep the name for a later sign-in, then sign
                // out, so Cancel always leaves the user signed out.
                writeProfileQuietly(UserProfileDto(created.uid, name, created.email ?: email))
                signOut()
            }
            val user = account.toUser(name = null, fallbackEmail = email)
            session.update { SessionData(userId = account.uid, email = user.email) }
            user
        }
    }

    override suspend fun saveProfile(name: String): AuthResult<Unit> = withContext(io) {
        val account = source.currentAccount()
            ?: return@withContext AuthResult.Failure(AuthFailure.SessionExpired)
        session.update { it.forAccount(account.uid).copy(name = name, profileSyncPending = true) }
        attempt(AuthOperation.Other) {
            source.writeProfile(UserProfileDto(account.uid, name, account.email))
            session.update { it.copy(profileSyncPending = false) }
        }
    }

    override suspend fun sendEmailConfirmation(): AuthResult<Unit> = withContext(io) {
        attempt(AuthOperation.Other) {
            source.sendEmailVerification()
            session.update { it.copy(confirmationNoticePending = true) }
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult<Unit> = withContext(io) {
        attempt(AuthOperation.PasswordReset, ignoredError = Unit) {
            source.sendPasswordResetEmail(email)
        }
    }

    override suspend fun refreshSession(): AuthResult<AuthUser> = withContext(io) {
        if (source.currentAccount() == null) {
            return@withContext AuthResult.Failure(AuthFailure.SessionExpired)
        }
        attempt(AuthOperation.Refresh) {
            val account = source.reload()
            var cached = session.data.first().forAccount(account.uid)
            if (cached.profileSyncPending && cached.name != null) {
                val synced = writeProfileQuietly(UserProfileDto(account.uid, cached.name, account.email))
                if (synced) cached = cached.copy(profileSyncPending = false)
            }
            val name = cached.name ?: readName(account.uid)
            val user = account.toUser(name, fallbackEmail = cached.email.orEmpty())
            val updated = cached.copy(
                name = name,
                email = user.email,
                emailConfirmed = account.emailVerified,
            )
            session.update { updated }
            user
        }
    }

    override suspend fun signOut() {
        try {
            source.signOut()
        } catch (e: Exception) {
            Timber.w("Service sign-out failed (%s); clearing local session anyway", e.javaClass.simpleName)
        }
        try {
            session.clear()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w("Clearing the session store failed (%s)", e.javaClass.simpleName)
        }
    }

    override suspend fun consumeConfirmationNotice(): String? = withContext(io) {
        val account = source.currentAccount() ?: return@withContext null
        val data = session.data.first()
        if (data.userId != account.uid || !data.confirmationNoticePending) return@withContext null
        session.update { it.copy(confirmationNoticePending = false) }
        data.email
    }

    /**
     * Awaits a request. If the caller is cancelled first, a later success is handed to
     * [onLateSuccess] in the application scope, which outlives any screen.
     */
    private suspend fun <T> AuthTask<T>.awaitOrCleanUp(onLateSuccess: suspend (T) -> Unit): T =
        try {
            await()
        } catch (e: CancellationException) {
            whenDone { result ->
                result.onSuccess { value -> appScope.launch { onLateSuccess(value) } }
            }
            throw e
        }

    /**
     * Runs [block], turning any SDK exception into a classified [AuthResult.Failure].
     * [ignoredError] is the value returned when the error means success for [operation].
     */
    private suspend fun <T> attempt(
        operation: AuthOperation,
        ignoredError: T? = null,
        block: suspend () -> T,
    ): AuthResult<T> = try {
        AuthResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val failure = mapper.map(e.toAuthErrorSignal(), operation)
        if (failure == null && ignoredError != null) {
            AuthResult.Success(ignoredError)
        } else {
            val classified = failure ?: AuthFailure.Unknown
            Timber.w("%s failed: %s", operation, classified)
            AuthResult.Failure(classified)
        }
    }

    private suspend fun readName(uid: String): String? = try {
        source.readProfile(uid)?.uName?.takeIf { it.isNotBlank() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w("Reading the profile failed (%s)", e.logLabel())
        null
    }

    /** Writes the profile; returns whether it worked. Never throws except on cancellation. */
    private suspend fun writeProfileQuietly(profile: UserProfileDto): Boolean = try {
        source.writeProfile(profile)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w("Writing the profile failed (%s)", e.logLabel())
        false
    }

    /** This cache if it belongs to [uid], otherwise an empty cache for [uid]. */
    private fun SessionData.forAccount(uid: String): SessionData =
        if (userId == uid) this else SessionData(userId = uid)

    private fun FirebaseAccount.toUser(name: String?, fallbackEmail: String) = AuthUser(
        id = uid,
        name = name,
        email = Email.normalize(email ?: fallbackEmail),
        emailConfirmed = emailVerified,
    )
}
