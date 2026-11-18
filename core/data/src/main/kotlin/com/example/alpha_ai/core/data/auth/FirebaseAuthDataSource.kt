package com.example.alpha_ai.core.data.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** The account as the sign-in service reports it. */
internal data class FirebaseAccount(
    val uid: String,
    val email: String?,
    val emailVerified: Boolean,
)

/**
 * A request already sent to the service. [await] suspends until it completes and is cancellable;
 * cancelling it does not stop the request, so [whenDone] still fires afterwards
 * (data-model.md "Cancellation").
 */
internal interface AuthTask<T> {
    suspend fun await(): T

    fun whenDone(block: (Result<T>) -> Unit)
}

/**
 * A thin wrapper over the Firebase SDK, so the repository can be tested with a fake. Every
 * failure is thrown as an [AuthServiceException], never as the SDK's own exception.
 */
internal interface FirebaseAuthDataSource {
    fun currentAccount(): FirebaseAccount?

    fun authStateChanges(): Flow<Boolean>

    fun signIn(email: String, password: String): AuthTask<FirebaseAccount>

    fun createUser(email: String, password: String): AuthTask<FirebaseAccount>

    /** Reloads the current user from the service; throws when there is none or it is invalid. */
    suspend fun reload(): FirebaseAccount

    suspend fun sendEmailVerification()

    suspend fun sendPasswordResetEmail(email: String)

    fun signOut()

    suspend fun readProfile(uid: String): UserProfileDto?

    suspend fun writeProfile(profile: UserProfileDto)
}

internal class FirebaseAuthDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : FirebaseAuthDataSource {

    override fun currentAccount(): FirebaseAccount? = auth.currentUser?.toAccount()

    override fun authStateChanges(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override fun signIn(email: String, password: String): AuthTask<FirebaseAccount> =
        TaskAdapter(auth.signInWithEmailAndPassword(email, password)) { result ->
            checkNotNull(result.user) { "Signed in without a user" }.toAccount()
        }

    override fun createUser(email: String, password: String): AuthTask<FirebaseAccount> =
        TaskAdapter(auth.createUserWithEmailAndPassword(email, password)) { result ->
            checkNotNull(result.user) { "Created without a user" }.toAccount()
        }

    override suspend fun reload(): FirebaseAccount = classified {
        val user = checkNotNull(auth.currentUser) { "No signed-in user" }
        user.reload().await()
        checkNotNull(auth.currentUser) { "User gone after reload" }.toAccount()
    }

    override suspend fun sendEmailVerification() = classified {
        useAppLanguage()
        checkNotNull(auth.currentUser) { "No signed-in user" }.sendEmailVerification().await()
        Unit
    }

    override suspend fun sendPasswordResetEmail(email: String) = classified {
        useAppLanguage()
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    override fun signOut() = auth.signOut()

    override suspend fun readProfile(uid: String): UserProfileDto? = classified {
        withProfileTimeout {
            firestore.collection(USERS).document(uid).get().await().toObject(UserProfileDto::class.java)
        }
    }

    /**
     * Offline, Firestore queues the write and its task completes only once it syncs, so the wait
     * is capped; the queued write still goes out later and the caller keeps the profile pending.
     */
    override suspend fun writeProfile(profile: UserProfileDto) = classified {
        withProfileTimeout { firestore.collection(USERS).document(profile.uid).set(profile).await() }
        Unit
    }

    /**
     * Not `withTimeoutOrNull`: a missing profile is a `null` result, and that must not be read as
     * a timeout.
     */
    private suspend fun <T> withProfileTimeout(block: suspend () -> T): T = try {
        withTimeout(PROFILE_TIMEOUT) { block() }
    } catch (e: TimeoutCancellationException) {
        throw AuthServiceException(AuthErrorSignal(AuthErrorSignal.Kind.Network))
    }

    /** Emails from the service arrive in the app's language (Arabic or English). */
    private fun useAppLanguage() {
        auth.setLanguageCode(if (Locale.getDefault().language == "ar") "ar" else "en")
    }

    private fun FirebaseUser.toAccount() = FirebaseAccount(uid, email, isEmailVerified)

    private class TaskAdapter<R, T>(
        private val task: Task<R>,
        private val transform: (R) -> T,
    ) : AuthTask<T> {
        override suspend fun await(): T = classified { transform(task.await()) }

        override fun whenDone(block: (Result<T>) -> Unit) {
            task.addOnCompleteListener { done ->
                val error = done.exception
                block(
                    if (done.isSuccessful) {
                        runCatching { transform(done.result) }
                    } else {
                        Result.failure(AuthServiceException((error ?: IllegalStateException()).toAuthErrorSignal()))
                    },
                )
            }
        }
    }

    private companion object {
        const val USERS = "users"
        val PROFILE_TIMEOUT = 10.seconds
    }
}

/** Runs an SDK call, rethrowing any failure as a message-free [AuthServiceException]. */
private inline fun <T> classified(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: AuthServiceException) {
    throw e
} catch (e: Exception) {
    throw AuthServiceException(e.toAuthErrorSignal())
}
