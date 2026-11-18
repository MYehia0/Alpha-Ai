package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthRepository
import javax.inject.Inject

/** The email for the one-time "confirmation link sent" message after sign-up, once (FR-008). */
class ConsumeConfirmationNoticeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): String? = authRepository.consumeConfirmationNotice()
}
