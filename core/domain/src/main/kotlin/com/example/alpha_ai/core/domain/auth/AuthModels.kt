package com.example.alpha_ai.core.domain.auth

/**
 * The signed-in account as the app sees it. [id] is the sign-in service's user id; it is never
 * shown or logged. [name] is `null` until known (an old account read offline, or a profile save
 * still pending). [email] is normalized ([Email.normalize]).
 */
data class AuthUser(
    val id: String,
    val name: String?,
    val email: String,
    val emailConfirmed: Boolean,
)

/**
 * Why an account operation failed, in terms a screen can explain to the user (FR-017). A
 * failure never carries a raw service message or error code.
 */
sealed interface AuthFailure {
    /** The device has no usable internet connection. */
    data object NoConnection : AuthFailure

    /** The email or the password is wrong. Never says which (FR-010). */
    data object InvalidCredentials : AuthFailure

    /** Sign-up with an email that already has an account. */
    data object EmailAlreadyInUse : AuthFailure

    /** The service rejected the password as too weak. */
    data object WeakPassword : AuthFailure

    /** The service rejected the email address. */
    data object InvalidEmail : AuthFailure

    /** The service is throttling this device or account. */
    data object TooManyAttempts : AuthFailure

    /** The account has been disabled. */
    data object AccountDisabled : AuthFailure

    /** The stored session is no longer valid; the user must sign in again. */
    data object SessionExpired : AuthFailure

    /** Anything that fits no other category. */
    data object Unknown : AuthFailure
}

/**
 * The outcome of an account operation: a value or a classified failure. Cancellation is not a
 * value: a cancelled caller gets `CancellationException`.
 */
sealed interface AuthResult<out T> {
    data class Success<out T>(val value: T) : AuthResult<T>

    data class Failure(val failure: AuthFailure) : AuthResult<Nothing>
}

/** Why the user is being shown sign-in instead of where they were. */
enum class SignInReason { SessionExpired, AccountDisabled, SignedOut }

/** Where the app starts, decided before the first frame (FR-001). */
sealed interface StartDestination {
    data object Introduction : StartDestination

    data class SignIn(val reason: SignInReason?) : StartDestination

    data object Home : StartDestination
}

/** Actions limited to once per [ActionCooldown.WINDOW]. */
enum class CooldownAction { PasswordReset, EmailConfirmation }
