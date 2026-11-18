package com.example.alpha_ai.feature.auth.navigation

import com.example.alpha_ai.core.domain.auth.SignInReason
import kotlinx.serialization.Serializable

/** The introduction pages shown on first launch (FR-003). */
@Serializable
data object IntroductionRoute

/** Sign-in. [reason] shows a notice saying why the user is here. */
@Serializable
data class SignInRoute(val reason: SignInReason? = null)

@Serializable
data object SignUpRoute

/** Password reset, with [email] prefilled from sign-in. */
@Serializable
data class PasswordResetRoute(val email: String = "")
