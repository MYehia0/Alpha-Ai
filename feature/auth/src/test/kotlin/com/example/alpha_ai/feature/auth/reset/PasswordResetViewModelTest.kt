package com.example.alpha_ai.feature.auth.reset

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.alpha_ai.core.domain.auth.ActionCooldown
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.CooldownAction
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.usecase.RequestPasswordResetUseCase
import com.example.alpha_ai.core.testing.auth.FakeAuthRepository
import com.example.alpha_ai.feature.auth.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class PasswordResetViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val time = TestTimeSource()
    private val cooldown = ActionCooldown(time)

    private fun viewModel(email: String = "ali@example.com") =
        PasswordResetViewModel(email, SavedStateHandle(), RequestPasswordResetUseCase(repository, cooldown))

    @Test
    fun `email is prefilled from the route`() {
        assertEquals("ali@example.com", viewModel().state.value.email)
    }

    @Test
    fun `sending shows the confirmation and starts the countdown`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(PasswordResetIntent.Submit)
        runCurrent()

        val state = viewModel.state.value
        assertTrue(state.sent)
        assertFalse(state.submitting)
        assertEquals(60, state.cooldownSeconds)
        assertEquals(listOf("ali@example.com"), repository.passwordResetCalls)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(59, viewModel.state.value.cooldownSeconds)
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.cooldownSeconds)
    }

    @Test
    fun `submit is ignored during the cooldown`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(PasswordResetIntent.Submit)
        runCurrent()
        assertFalse(viewModel.state.value.canSubmit)
        viewModel.onIntent(PasswordResetIntent.Submit)
        runCurrent()
        assertEquals(1, repository.passwordResetCalls.size)
    }

    @Test
    fun `a cooldown from an earlier screen is shown on open`() = runTest(main.dispatcher) {
        cooldown.start(CooldownAction.PasswordReset)
        time += 20.seconds
        assertEquals(40, viewModel().state.value.cooldownSeconds)
    }

    @Test
    fun `no connection is shown and nothing is marked sent`() = runTest(main.dispatcher) {
        repository.passwordResetResult = AuthResult.Failure(AuthFailure.NoConnection)
        val viewModel = viewModel()
        viewModel.onIntent(PasswordResetIntent.Submit)
        runCurrent()
        val state = viewModel.state.value
        assertEquals(AuthFailure.NoConnection, state.failure)
        assertFalse(state.sent)
        assertEquals(0, state.cooldownSeconds)
    }

    @Test
    fun `an invalid email shows a field error`() = runTest(main.dispatcher) {
        val viewModel = viewModel(email = "")
        viewModel.onIntent(PasswordResetIntent.Submit)
        runCurrent()
        assertEquals(FieldError.Empty, viewModel.state.value.fieldErrors[FormField.Email])
        assertTrue(repository.passwordResetCalls.isEmpty())
    }

    @Test
    fun `cancel while submitting keeps the email and is not sent`() = runTest(main.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.passwordResetGate = gate
        val viewModel = viewModel()
        viewModel.onIntent(PasswordResetIntent.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.cancellable)

        viewModel.onIntent(PasswordResetIntent.Cancel)
        gate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.submitting)
        assertFalse(state.sent)
        assertEquals("ali@example.com", state.email)
    }

    @Test
    fun `back to sign in`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onIntent(PasswordResetIntent.BackToSignIn)
            assertEquals(PasswordResetEffect.ToSignIn, awaitItem())
        }
    }
}
