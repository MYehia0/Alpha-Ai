package com.example.alpha_ai.core.domain.auth.usecase

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.AuthResult
import com.example.alpha_ai.core.domain.auth.AuthUser
import com.example.alpha_ai.core.domain.auth.Email
import com.example.alpha_ai.core.domain.auth.SignUpForm
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * A finished sign-up. [unshownFailures] are failures of the steps after the account exists
 * (saving the name, sending the confirmation email): the user is not told, so the caller logs
 * their case names (constitution VIII).
 */
data class SignUpSuccess(val user: AuthUser, val unshownFailures: List<AuthFailure> = emptyList())

/** Creates an account (research.md §8). */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    /**
     * [onAccountCreated] is called once the account exists; from then on the remaining steps
     * finish even if the caller is cancelled, so the user is never left half signed up.
     */
    suspend operator fun invoke(
        form: SignUpForm,
        onAccountCreated: () -> Unit = {},
    ): FormSubmission<SignUpSuccess> {
        val normalized = form.copy(email = Email.normalize(form.email), name = form.name.trim())
        val errors = normalized.validate()
        if (errors.isNotEmpty()) return FormSubmission.FieldErrors(errors)

        val created = authRepository.createAccount(normalized.email, normalized.password, normalized.name)
        if (created is AuthResult.Failure) return FormSubmission.Submitted(created)
        val user = (created as AuthResult.Success).value
        onAccountCreated()

        return withContext(NonCancellable) {
            val unshown = buildList {
                (authRepository.saveProfile(normalized.name) as? AuthResult.Failure)?.let { add(it.failure) }
                (authRepository.sendEmailConfirmation() as? AuthResult.Failure)?.let { add(it.failure) }
            }
            FormSubmission.Submitted(AuthResult.Success(SignUpSuccess(user.copy(name = normalized.name), unshown)))
        }
    }
}
