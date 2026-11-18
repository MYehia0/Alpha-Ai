package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AppPreferencesRepository
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.LegacyCleanup
import com.example.alpha_ai.core.domain.auth.StartDestination
import javax.inject.Inject

/**
 * Where the app starts (FR-001, FR-002). Local only, no network. A signed-in user also counts
 * as having seen the introduction, so a v1 user who signs out later is not shown it.
 */
class ResolveStartDestinationUseCase @Inject constructor(
    private val legacyCleanup: LegacyCleanup,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferencesRepository,
) {
    suspend operator fun invoke(): StartDestination {
        legacyCleanup.runOnce()
        return when {
            authRepository.currentUser() != null -> {
                preferences.setIntroductionCompleted()
                StartDestination.Home
            }
            preferences.isIntroductionCompleted() -> StartDestination.SignIn(reason = null)
            else -> StartDestination.Introduction
        }
    }
}
