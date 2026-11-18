package com.example.alpha_ai.core.testing.auth

import com.example.alpha_ai.core.domain.auth.AppPreferencesRepository

/** An in-memory [AppPreferencesRepository]. */
class FakeAppPreferencesRepository(
    var introductionCompleted: Boolean = false,
) : AppPreferencesRepository {

    var setIntroductionCompletedCalls = 0

    override suspend fun isIntroductionCompleted(): Boolean = introductionCompleted

    override suspend fun setIntroductionCompleted() {
        setIntroductionCompletedCalls++
        introductionCompleted = true
    }
}
