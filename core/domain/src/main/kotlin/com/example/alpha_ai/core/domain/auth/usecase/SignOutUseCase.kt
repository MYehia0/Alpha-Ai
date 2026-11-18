package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthRepository
import javax.inject.Inject

/**
 * Signs out on this phone (FR-016). Works offline and never fails. Whether the introduction was
 * seen is a per-phone preference and is kept.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.signOut()
}
