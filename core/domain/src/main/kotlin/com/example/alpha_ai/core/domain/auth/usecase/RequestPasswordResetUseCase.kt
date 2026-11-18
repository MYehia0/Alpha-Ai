package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.ActionCooldown
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.CooldownAction
import com.example.alpha_ai.core.domain.auth.Email
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.PasswordResetForm
import javax.inject.Inject
import kotlin.time.Duration

sealed interface PasswordResetResult {
    /** Sent, or the email has no account: the user is told the same either way (FR-013). */
    data object Sent : PasswordResetResult

    /** A reset was sent less than a minute ago (FR-014). */
    data class CoolingDown(val remaining: Duration) : PasswordResetResult

    data class FieldErrors(val errors: Map<FormField, FieldError>) : PasswordResetResult

    data class Failure(val failure: AuthFailure) : PasswordResetResult
}

/** Requests a password reset email, at most once per [ActionCooldown.WINDOW] (FR-012–FR-014). */
class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val cooldown: ActionCooldown,
) {
    suspend operator fun invoke(email: String): PasswordResetResult {
        val normalized = Email.normalize(email)
        val errors = PasswordResetForm(normalized).validate()
        if (errors.isNotEmpty()) return PasswordResetResult.FieldErrors(errors)

        val remaining = cooldown.remaining(CooldownAction.PasswordReset)
        if (remaining.isPositive()) return PasswordResetResult.CoolingDown(remaining)

        return when (val result = authRepository.sendPasswordReset(normalized)) {
            is AuthResult.Success -> {
                cooldown.start(CooldownAction.PasswordReset)
                PasswordResetResult.Sent
            }
            is AuthResult.Failure -> PasswordResetResult.Failure(result.failure)
        }
    }

    /** Time left before another reset may be sent; [Duration.ZERO] when allowed now. */
    fun remainingCooldown(): Duration = cooldown.remaining(CooldownAction.PasswordReset)
}
