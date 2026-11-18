package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AppPreferencesRepository
import javax.inject.Inject

/** Records that the introduction was finished or skipped, so it is not shown again (FR-004). */
class CompleteIntroductionUseCase @Inject constructor(
    private val preferences: AppPreferencesRepository,
) {
    /** Never throws: the repository logs a failed write (at worst the introduction shows again). */
    suspend operator fun invoke() = preferences.setIntroductionCompleted()
}
