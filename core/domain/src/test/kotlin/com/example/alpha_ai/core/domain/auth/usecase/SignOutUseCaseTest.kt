package com.example.alpha_ai.core.domain.auth.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignOutUseCaseTest {

    @Test
    fun `signs out and leaves the introduction status alone`() = runTest {
        val repository = StubAuthRepository(USER)
        val preferences = StubPreferences(introductionCompleted = true)
        SignOutUseCase(repository)()
        assertEquals(1, repository.signOutCalls)
        assertNull(repository.currentUser())
        assertTrue(preferences.isIntroductionCompleted())
    }
}
