package com.example.alpha_ai.core.data.auth

import com.example.alpha_ai.core.datastore.SessionData
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepositoryTest {

    private val source = FakeFirebaseAuthDataSource()
    private val store = InMemorySessionStore()
    private val account = FirebaseAccount(uid = "uid-1", email = "ali@example.com", emailVerified = false)

    private fun TestScope.repository() = FirebaseAuthRepository(
        source = source,
        session = store,
        mapper = FirebaseErrorMapper(),
        io = StandardTestDispatcher(testScheduler),
        appScope = backgroundScope,
    )

    private fun authError(code: String) =
        AuthServiceException(AuthErrorSignal(AuthErrorSignal.Kind.Auth, code))

    private fun offline() = AuthServiceException(AuthErrorSignal(AuthErrorSignal.Kind.Network))

    @Test
    fun `sign-in success caches the profile with the stored name`() = runTest {
        source.profiles["uid-1"] = UserProfileDto("uid-1", "Ali", "ali@example.com")
        source.signInTask = FakeAuthTask.succeeded(account)
        val result = repository().signIn("ali@example.com", "secret12")

        val user = (result as AuthResult.Success).value
        assertEquals("Ali", user.name)
        assertEquals(SessionData(userId = "uid-1", name = "Ali", email = "ali@example.com"), store.state.value)
    }

    @Test
    fun `sign-in network failure is no connection`() = runTest {
        source.signInTask = FakeAuthTask.failed(offline())
        assertEquals(
            AuthResult.Failure(AuthFailure.NoConnection),
            repository().signIn("ali@example.com", "secret12"),
        )
    }

    @Test
    fun `wrong password is invalid credentials`() = runTest {
        source.signInTask = FakeAuthTask.failed(authError("ERROR_INVALID_CREDENTIAL"))
        assertEquals(
            AuthResult.Failure(AuthFailure.InvalidCredentials),
            repository().signIn("ali@example.com", "wrong"),
        )
    }

    @Test
    fun `profile write failure is remembered as pending`() = runTest {
        source.account = account
        source.writeProfileError = offline()
        val result = repository().saveProfile("Ali")

        assertEquals(AuthResult.Failure(AuthFailure.NoConnection), result)
        assertEquals("Ali", store.state.value.name)
        assertTrue(store.state.value.profileSyncPending)
    }

    @Test
    fun `refresh retries a pending profile sync`() = runTest {
        source.account = account
        store.state.value = SessionData(userId = "uid-1", name = "Ali", profileSyncPending = true)
        val result = repository().refreshSession()

        assertTrue(result is AuthResult.Success)
        assertEquals(UserProfileDto("uid-1", "Ali", "ali@example.com"), source.profiles["uid-1"])
        assertFalse(store.state.value.profileSyncPending)
    }

    @Test
    fun `refresh of a disabled account is account disabled`() = runTest {
        source.account = account
        source.reloadError = authError("ERROR_USER_DISABLED")
        assertEquals(AuthResult.Failure(AuthFailure.AccountDisabled), repository().refreshSession())
    }

    @Test
    fun `refresh without a signed-in user is session expired`() = runTest {
        assertEquals(AuthResult.Failure(AuthFailure.SessionExpired), repository().refreshSession())
    }

    @Test
    fun `reset for an unknown email is success`() = runTest {
        source.resetError = authError("ERROR_USER_NOT_FOUND")
        assertEquals(AuthResult.Success(Unit), repository().sendPasswordReset("nobody@example.com"))
    }

    @Test
    fun `reset while offline is no connection`() = runTest {
        source.resetError = offline()
        assertEquals(
            AuthResult.Failure(AuthFailure.NoConnection),
            repository().sendPasswordReset("ali@example.com"),
        )
    }

    @Test
    fun `sign-out clears the store even when the service throws`() = runTest {
        store.state.value = SessionData(userId = "uid-1", name = "Ali")
        source.signOutError = IllegalStateException("boom")
        repository().signOut()

        assertEquals(1, source.signOutCalls)
        assertEquals(SessionData.EMPTY, store.state.value)
    }

    @Test
    fun `a cache for another account is ignored`() = runTest {
        store.state.value = SessionData(userId = "someone-else", name = "Sara")
        source.account = account
        val repository = repository()
        runCurrent()
        assertNull(repository.currentUser()?.name)
    }

    @Test
    fun `confirmation notice is returned once`() = runTest {
        source.account = account
        store.state.value = SessionData(userId = "uid-1", email = "ali@example.com")
        val repository = repository()
        assertEquals(AuthResult.Success(Unit), repository.sendEmailConfirmation())

        assertEquals("ali@example.com", repository.consumeConfirmationNotice())
        assertNull(repository.consumeConfirmationNotice())
    }

    @Test
    fun `sign-in cancelled then completing is signed out`() = runTest {
        val task = FakeAuthTask<FirebaseAccount>()
        source.signInTask = task
        val repository = repository()
        val call = launch { repository.signIn("ali@example.com", "secret12") }
        runCurrent()
        call.cancel()
        runCurrent()

        source.account = account
        task.succeed(account)
        runCurrent()

        assertEquals(1, source.signOutCalls)
        assertEquals(1, store.clearCalls)
    }

    @Test
    fun `create-account cancelled then completing saves the name and signs out`() = runTest {
        val task = FakeAuthTask<FirebaseAccount>()
        source.createTask = task
        val repository = repository()
        val call = launch { repository.createAccount("ali@example.com", "secret12", "Ali") }
        runCurrent()
        call.cancel()
        runCurrent()

        source.account = account
        task.succeed(account)
        runCurrent()

        assertEquals(UserProfileDto("uid-1", "Ali", "ali@example.com"), source.profiles["uid-1"])
        assertEquals(1, source.signOutCalls)
    }

    @Test
    fun `cancelled then failing does nothing`() = runTest {
        val task = FakeAuthTask<FirebaseAccount>()
        source.signInTask = task
        val repository = repository()
        val call = launch { repository.signIn("ali@example.com", "secret12") }
        runCurrent()
        call.cancel()
        task.fail(authError("ERROR_INVALID_CREDENTIAL"))
        runCurrent()

        assertEquals(0, source.signOutCalls)
    }

    @Test
    fun `cancellation reaches the caller`() = runTest {
        source.signInTask = FakeAuthTask()
        val repository = repository()
        val call = async { repository.signIn("ali@example.com", "secret12") }
        runCurrent()
        call.cancel()
        runCurrent()

        assertTrue(call.isCancelled)
        assertTrue(runCatching { call.await() }.exceptionOrNull() is CancellationException)
    }
}
