package com.example.alpha_ai.core.data.auth

import com.example.alpha_ai.core.datastore.AppPreferencesStore
import com.example.alpha_ai.core.datastore.SessionData
import com.example.alpha_ai.core.datastore.SessionStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** An [AuthTask] completed by the test. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class FakeAuthTask<T> : AuthTask<T> {
    private val result = CompletableDeferred<T>()
    private val listeners = mutableListOf<(Result<T>) -> Unit>()

    override suspend fun await(): T = result.await()

    override fun whenDone(block: (Result<T>) -> Unit) {
        if (result.isCompleted) block(runCatching { result.getCompleted() }) else listeners += block
    }

    fun succeed(value: T) {
        result.complete(value)
        listeners.forEach { it(Result.success(value)) }
    }

    fun fail(error: Throwable) {
        result.completeExceptionally(error)
        listeners.forEach { it(Result.failure(error)) }
    }

    companion object {
        fun <T> succeeded(value: T) = FakeAuthTask<T>().apply { succeed(value) }

        fun <T> failed(error: Throwable) = FakeAuthTask<T>().apply { fail(error) }
    }
}

/** A scriptable [FirebaseAuthDataSource]. */
internal class FakeFirebaseAuthDataSource : FirebaseAuthDataSource {
    var account: FirebaseAccount? = null
    val signedIn = MutableStateFlow(false)
    var signInTask: AuthTask<FirebaseAccount> = FakeAuthTask()
    var createTask: AuthTask<FirebaseAccount> = FakeAuthTask()
    var reloadError: Throwable? = null
    var resetError: Throwable? = null
    var verificationError: Throwable? = null
    var signOutError: Throwable? = null
    var writeProfileError: Throwable? = null
    val profiles = mutableMapOf<String, UserProfileDto>()

    var signOutCalls = 0
    var verificationCalls = 0
    val writtenProfiles = mutableListOf<UserProfileDto>()

    override fun currentAccount() = account

    override fun authStateChanges(): Flow<Boolean> = signedIn

    override fun signIn(email: String, password: String) = signInTask

    override fun createUser(email: String, password: String) = createTask

    override suspend fun reload(): FirebaseAccount {
        reloadError?.let { throw it }
        return checkNotNull(account)
    }

    override suspend fun sendEmailVerification() {
        verificationCalls++
        verificationError?.let { throw it }
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        resetError?.let { throw it }
    }

    override fun signOut() {
        signOutCalls++
        account = null
        signOutError?.let { throw it }
    }

    override suspend fun readProfile(uid: String) = profiles[uid]

    override suspend fun writeProfile(profile: UserProfileDto) {
        writtenProfiles += profile
        writeProfileError?.let { throw it }
        profiles[profile.uid] = profile
    }
}

/** An in-memory [SessionStore]. */
internal class InMemorySessionStore(initial: SessionData = SessionData.EMPTY) : SessionStore {
    val state = MutableStateFlow(initial)
    var clearCalls = 0

    override val data: Flow<SessionData> = state

    override suspend fun update(transform: (SessionData) -> SessionData) {
        state.value = transform(state.value)
    }

    override suspend fun clear() {
        clearCalls++
        state.value = SessionData.EMPTY
    }
}

/** An in-memory [AppPreferencesStore] that can be made to fail. */
internal class InMemoryAppPreferencesStore : AppPreferencesStore {
    private val introduction = MutableStateFlow(false)
    private val cleanup = MutableStateFlow(false)
    var failure: Exception? = null

    override val introductionCompleted: Flow<Boolean> =
        introduction.map { failure?.let { throw it } ?: it }

    override val legacyCleanupDone: Flow<Boolean> = cleanup.map { failure?.let { throw it } ?: it }

    override suspend fun setIntroductionCompleted() {
        failure?.let { throw it }
        introduction.value = true
    }

    override suspend fun setLegacyCleanupDone() {
        failure?.let { throw it }
        cleanup.value = true
    }
}
