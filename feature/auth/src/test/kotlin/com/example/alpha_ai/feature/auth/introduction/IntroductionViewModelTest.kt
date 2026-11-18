package com.example.alpha_ai.feature.auth.introduction

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.alpha_ai.core.domain.auth.AppPreferencesRepository
import com.example.alpha_ai.core.domain.auth.usecase.CompleteIntroductionUseCase
import com.example.alpha_ai.core.testing.auth.FakeAppPreferencesRepository
import com.example.alpha_ai.feature.auth.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IntroductionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val preferences = FakeAppPreferencesRepository()

    private fun viewModel(repository: AppPreferencesRepository = preferences) =
        IntroductionViewModel(SavedStateHandle(), CompleteIntroductionUseCase(repository))

    @Test
    fun `pages stay within bounds`() {
        val viewModel = viewModel()
        viewModel.onIntent(IntroductionIntent.PreviousPage)
        assertEquals(0, viewModel.state.value.page)
        repeat(5) { viewModel.onIntent(IntroductionIntent.NextPage) }
        assertEquals(2, viewModel.state.value.page)
        assertTrue(viewModel.state.value.isLastPage)
        viewModel.onIntent(IntroductionIntent.GoToPage(1))
        assertEquals(1, viewModel.state.value.page)
        viewModel.onIntent(IntroductionIntent.GoToPage(7))
        assertEquals(2, viewModel.state.value.page)
    }

    @Test
    fun `skip completes and goes to sign-in once`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onIntent(IntroductionIntent.Skip)
            viewModel.onIntent(IntroductionIntent.Skip)
            assertEquals(IntroductionEffect.ToSignIn, awaitItem())
            advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(preferences.introductionCompleted)
        assertEquals(1, preferences.setIntroductionCompletedCalls)
    }

    @Test
    fun `finish completes and goes to sign-in`() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onIntent(IntroductionIntent.Finish)
            assertEquals(IntroductionEffect.ToSignIn, awaitItem())
        }
        assertTrue(preferences.introductionCompleted)
    }

    @Test
    fun `a save that does not stick still goes to sign-in`() = runTest(main.dispatcher) {
        // The real repository logs and swallows a failed write; it is modelled as a no-op.
        val forgetful = object : AppPreferencesRepository {
            override suspend fun isIntroductionCompleted() = false

            override suspend fun setIntroductionCompleted() = Unit
        }
        val viewModel = viewModel(forgetful)
        viewModel.effects.test {
            viewModel.onIntent(IntroductionIntent.Finish)
            assertEquals(IntroductionEffect.ToSignIn, awaitItem())
        }
    }
}
