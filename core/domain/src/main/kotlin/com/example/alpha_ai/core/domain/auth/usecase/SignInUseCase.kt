package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.AuthUser
import com.example.alpha_ai.core.domain.auth.Email
import com.example.alpha_ai.core.domain.auth.SignInForm
import javax.inject.Inject

/** Signs in with email and password (FR-009, FR-020). */
class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): FormSubmission<AuthUser> {
        val normalized = Email.normalize(email)
        val errors = SignInForm(normalized, password).validate()
        if (errors.isNotEmpty()) return FormSubmission.FieldErrors(errors)
        return FormSubmission.Submitted(authRepository.signIn(normalized, password))
    }
}
