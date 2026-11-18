package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.StartDestination
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveStartDestinationUseCaseTest {

    private val cleanup = CountingCleanup()

    private fun resolve(repository: StubAuthRepository, preferences: StubPreferences) =
        ResolveStartDestinationUseCase(cleanup, repository, preferences)

    @Test
    fun `signed-in user goes home and counts as introduced`() = runTest {
        val preferences = StubPreferences(introductionCompleted = false)
        assertEquals(StartDestination.Home, resolve(StubAuthRepository(USER), preferences)())
        assertTrue(preferences.introductionCompleted)
    }

    @Test
    fun `signed-out user who saw the introduction goes to sign-in`() = runTest {
        assertEquals(
            StartDestination.SignIn(reason = null),
            resolve(StubAuthRepository(), StubPreferences(introductionCompleted = true))(),
        )
    }

    @Test
    fun `first launch shows the introduction`() = runTest {
        assertEquals(StartDestination.Introduction, resolve(StubAuthRepository(), StubPreferences())())
    }

    @Test
    fun `legacy clean-up runs before deciding`() = runTest {
        resolve(StubAuthRepository(), StubPreferences())()
        assertEquals(1, cleanup.runs)
    }
}
