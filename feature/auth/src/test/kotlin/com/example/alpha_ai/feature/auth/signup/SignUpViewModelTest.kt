package com.example.alpha_ai.feature.auth.signup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.usecase.SignUpUseCase
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

class SignUpViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val savedState = SavedStateHandle()

    private fun viewModel(handle: SavedStateHandle = savedState) = SignUpViewModel(handle, SignUpUseCase(repository))

    private fun SignUpViewModel.fill(
        name: String = "Ali",
        email: String = "ali@example.com",
        password: String = "secret12",
        confirm: String = password,
    ) {
        onIntent(SignUpIntent.NameChanged(name))
        onIntent(SignUpIntent.EmailChanged(email))
        onIntent(SignUpIntent.PasswordChanged(password))
        onIntent(SignUpIntent.ConfirmPasswordChanged(confirm))
    }

    @Test
    fun `invalid fields show errors and send nothing`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.fill(name = " ", password = "short")
        viewModel.onIntent(SignUpIntent.Submit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(FieldError.Empty, state.fieldErrors[FormField.Name])
        assertEquals(FieldError.TooShort, state.fieldErrors[FormField.Password])
        assertFalse(state.submitting)
        assertTrue(repository.createAccountCalls.isEmpty())
    }

    @Test
    fun `editing a field clears its error`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(SignUpIntent.Submit)
        advanceUntilIdle()
        viewModel.onIntent(SignUpIntent.NameChanged("Ali"))
        assertNull(viewModel.state.value.fieldErrors[FormField.Name])
        assertEquals(FieldError.Empty, viewModel.state.value.fieldErrors[FormField.Email])
    }

    @Test
    fun `success authenticates`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.fill()
            viewModel.onIntent(SignUpIntent.Submit)
            assertEquals(SignUpEffect.Authenticated, awaitItem())
        }
        assertEquals(listOf("Ali"), repository.saveProfileCalls)
        assertEquals(1, repository.emailConfirmationCalls)
    }

    @Test
    fun `email already in use shows the failure and keeps the text`() = runTest(main.dispatcher) {
        repository.createAccountResult = AuthResult.Failure(AuthFailure.EmailAlreadyInUse)
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onIntent(SignUpIntent.Submit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthFailure.EmailAlreadyInUse, state.failure)
        assertEquals("ali@example.com", state.email)
        assertEquals("secret12", state.password)
        assertFalse(state.submitting)
    }

    @Test
    fun `double submit sends once`() = runTest(main.dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onIntent(SignUpIntent.Submit)
        viewModel.onIntent(SignUpIntent.Submit)
        runCurrent()
        assertEquals(1, repository.createAccountCalls.size)
        repository.gate?.complete(Unit)
    }

    @Test
    fun `name and email survive recreation, passwords do not`() = runTest(main.dispatcher) {
        viewModel().fill()
        val restored = viewModel(savedState).state.value
        assertEquals("Ali", restored.name)
        assertEquals("ali@example.com", restored.email)
        assertEquals("", restored.password)
        assertTrue(savedState.keys().none { it.contains("password", ignoreCase = true) })
    }

    @Test
    fun `cancel during account creation stops the request and keeps the fields`() = runTest(main.dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.fill()
            viewModel.onIntent(SignUpIntent.Submit)
            runCurrent()
            assertTrue(viewModel.state.value.cancellable)

            viewModel.onIntent(SignUpIntent.Cancel)
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
    fun `cancel is hidden once the account exists and the sign-up finishes`() = runTest(main.dispatcher) {
        repository.profileGate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.fill()
            viewModel.onIntent(SignUpIntent.Submit)
            runCurrent()
            assertTrue(viewModel.state.value.submitting)
            assertFalse(viewModel.state.value.cancellable)

            viewModel.onIntent(SignUpIntent.Cancel)
            assertTrue(viewModel.state.value.submitting)
            repository.profileGate?.complete(Unit)
            assertEquals(SignUpEffect.Authenticated, awaitItem())
        }
    }
}
