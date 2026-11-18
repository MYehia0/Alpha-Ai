package com.example.alpha_ai.feature.auth.signin

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.core.domain.auth.usecase.SignInUseCase
import com.example.alpha_ai.core.testing.auth.FakeAuthRepository
import com.example.alpha_ai.feature.auth.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val savedState = SavedStateHandle()

    private fun viewModel(reason: SignInReason? = null, handle: SavedStateHandle = savedState) =
        SignInViewModel(reason, handle, SignInUseCase(repository))

    private fun SignInViewModel.fill(email: String = "ali@example.com", password: String = "secret12") {
        onIntent(SignInIntent.EmailChanged(email))
        onIntent(SignInIntent.PasswordChanged(password))
    }

    @Test
    fun `success authenticates with the normalized email`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.fill(email = " Ali@Example.com ")
            viewModel.onIntent(SignInIntent.Submit)
            assertTrue(viewModel.state.value.submitting)
            assertEquals(SignInEffect.Authenticated, awaitItem())
        }
        assertEquals(listOf("ali@example.com" to "secret12"), repository.signInCalls)
    }

    @Test
    fun `empty submit shows field errors and sends nothing`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(SignInIntent.Submit)
        advanceUntilIdle()
        assertEquals(FieldError.Empty, viewModel.state.value.fieldErrors[FormField.Email])
        assertEquals(FieldError.Empty, viewModel.state.value.fieldErrors[FormField.Password])
        assertTrue(repository.signInCalls.isEmpty())
    }

    @Test
    fun `each failure is shown and the fields are kept`() = runTest(main.dispatcher) {
        listOf(
            AuthFailure.InvalidCredentials,
            AuthFailure.NoConnection,
            AuthFailure.TooManyAttempts,
            AuthFailure.AccountDisabled,
            AuthFailure.Unknown,
        ).forEach { failure ->
            repository.signInResult = AuthResult.Failure(failure)
            val viewModel = viewModel()
            viewModel.fill()
            viewModel.onIntent(SignInIntent.Submit)
            advanceUntilIdle()
            val state = viewModel.state.value
            assertEquals(failure, state.failure)
            assertFalse(state.submitting)
            assertEquals("secret12", state.password)
        }
    }

    @Test
    fun `editing clears the failure`() = runTest(main.dispatcher) {
        repository.signInResult = AuthResult.Failure(AuthFailure.InvalidCredentials)
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onIntent(SignInIntent.Submit)
        advanceUntilIdle()
        viewModel.onIntent(SignInIntent.PasswordChanged("secret13"))
        assertNull(viewModel.state.value.failure)
    }

    @Test
    fun `double submit sends once`() = runTest(main.dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onIntent(SignInIntent.Submit)
        viewModel.onIntent(SignInIntent.Submit)
        runCurrent()
        assertEquals(1, repository.signInCalls.size)
        repository.gate?.complete(Unit)
    }

    @Test
    fun `the previous failure stays until the new result, so the layout does not shift`() = runTest(main.dispatcher) {
        repository.signInResult = AuthResult.Failure(AuthFailure.InvalidCredentials)
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onIntent(SignInIntent.Submit)
        advanceUntilIdle()

        repository.signInResult = AuthResult.Failure(AuthFailure.NoConnection)
        repository.gate = CompletableDeferred()
        viewModel.onIntent(SignInIntent.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.submitting)
        assertEquals(AuthFailure.InvalidCredentials, viewModel.state.value.failure)

        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(AuthFailure.NoConnection, viewModel.state.value.failure)
    }

    @Test
    fun `cancel while submitting stops the request and keeps the fields`() = runTest(main.dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.fill()
            viewModel.onIntent(SignInIntent.Submit)
            runCurrent()
            assertTrue(viewModel.state.value.cancellable)
            viewModel.onIntent(SignInIntent.Cancel)
            runCurrent()
            repository.gate?.complete(Unit)
            advanceUntilIdle()
            expectNoEvents()
        }
        val state = viewModel.state.value
        assertFalse(state.submitting)
        assertEquals("ali@example.com", state.email)
        assertEquals("secret12", state.password)
        assertEquals(1, repository.cancelledCalls)
    }

    @Test
    fun `notice comes from the route and stays dismissed after recreation`() {
        val viewModel = viewModel(reason = SignInReason.SessionExpired)
        assertEquals(SignInReason.SessionExpired, viewModel.state.value.notice)
        viewModel.onIntent(SignInIntent.DismissNotice)
        assertNull(viewModel.state.value.notice)
        assertNull(viewModel(reason = SignInReason.SessionExpired).state.value.notice)
    }

    @Test
    fun `forgot password carries the typed email`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onIntent(SignInIntent.EmailChanged(" ali@example.com "))
            viewModel.onIntent(SignInIntent.ForgotPassword)
            assertEquals(SignInEffect.ToPasswordReset("ali@example.com"), awaitItem())
            viewModel.onIntent(SignInIntent.CreateAccount)
            assertEquals(SignInEffect.ToSignUp, awaitItem())
        }
    }

    @Test
    fun `the email survives recreation, the password does not`() {
        viewModel().fill()
        val restored = viewModel().state.value
        assertEquals("ali@example.com", restored.email)
        assertEquals("", restored.password)
    }
}
