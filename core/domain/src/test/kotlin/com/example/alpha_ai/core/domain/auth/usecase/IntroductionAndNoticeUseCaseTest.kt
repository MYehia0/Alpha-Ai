package com.example.alpha_ai.core.domain.auth.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroductionAndNoticeUseCaseTest {

    @Test
    fun `completing the introduction sets the flag`() = runTest {
        val preferences = StubPreferences()
        CompleteIntroductionUseCase(preferences)()
        assertTrue(preferences.introductionCompleted)
    }

    @Test
    fun `the confirmation notice is returned once`() = runTest {
        val repository = StubAuthRepository().apply { notice = "ali@example.com" }
        val consume = ConsumeConfirmationNoticeUseCase(repository)
        assertEquals("ali@example.com", consume())
        assertNull(consume())
    }
}
