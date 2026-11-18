package com.example.alpha_ai.shell

import androidx.lifecycle.SavedStateHandle
import com.example.alpha_ai.core.domain.auth.LegacyCleanup
import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.core.domain.auth.StartDestination
import com.example.alpha_ai.core.domain.auth.usecase.ResolveStartDestinationUseCase
import com.example.alpha_ai.core.testing.auth.FakeAppPreferencesRepository
import com.example.alpha_ai.core.testing.auth.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val preferences = FakeAppPreferencesRepository()

    /** Clean-up that can be held to simulate slow storage. */
    private class GatedCleanup : LegacyCleanup {
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun runOnce() {
            gate?.await()
        }
    }

    private val cleanup = GatedCleanup()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(reason: String? = null) = StartViewModel(
        SavedStateHandle(if (reason == null) emptyMap() else mapOf(StartViewModel.EXTRA_START_REASON to reason)),
        ResolveStartDestinationUseCase(cleanup, repository, preferences),
        repository,
    )

    private fun TestScope.resolved(viewModel: StartViewModel): StartDestination? {
        advanceUntilIdle()
        return (viewModel.state.value as? StartState.Resolved)?.destination
    }

    @Test
    fun `resolving until decided`() = runTest(dispatcher) {
        cleanup.gate = CompletableDeferred()
        val viewModel = viewModel()
        runCurrent()
        assertEquals(StartState.Resolving, viewModel.state.value)
    }

    @Test
    fun `signed in goes home`() = runTest(dispatcher) {
        repository.signInAs(FakeAuthRepository.TEST_USER)
        assertEquals(StartDestination.Home, resolved(viewModel()))
    }

    @Test
    fun `introduced and signed out goes to sign-in`() = runTest(dispatcher) {
        preferences.introductionCompleted = true
        assertEquals(StartDestination.SignIn(null), resolved(viewModel()))
    }

    @Test
    fun `first launch goes to the introduction`() = runTest(dispatcher) {
        assertEquals(StartDestination.Introduction, resolved(viewModel()))
    }

    @Test
    fun `slow storage with a signed-in user still goes home`() = runTest(dispatcher) {
        cleanup.gate = CompletableDeferred()
        repository.signInAs(FakeAuthRepository.TEST_USER)
        val viewModel = viewModel()
        advanceTimeBy(StartViewModel.RESOLVE_TIMEOUT.inWholeMilliseconds + 1)
        assertEquals(StartState.Resolved(StartDestination.Home), viewModel.state.value)
    }

    @Test
    fun `slow storage without a user goes to sign-in`() = runTest(dispatcher) {
        cleanup.gate = CompletableDeferred()
        val viewModel = viewModel()
        advanceTimeBy(StartViewModel.RESOLVE_TIMEOUT.inWholeMilliseconds + 1)
        assertEquals(StartState.Resolved(StartDestination.SignIn(null)), viewModel.state.value)
    }

    @Test
    fun `a valid start reason skips resolution`() = runTest(dispatcher) {
        repository.signInAs(FakeAuthRepository.TEST_USER)
        val viewModel = viewModel(reason = "SessionExpired")
        assertEquals(StartState.Resolved(StartDestination.SignIn(SignInReason.SessionExpired)), viewModel.state.value)
    }

    @Test
    fun `an invalid start reason is ignored`() = runTest(dispatcher) {
        assertEquals(StartDestination.Introduction, resolved(viewModel(reason = "Hacked")))
        assertEquals(StartDestination.Introduction, resolved(viewModel(reason = "sessionexpired")))
    }
}
