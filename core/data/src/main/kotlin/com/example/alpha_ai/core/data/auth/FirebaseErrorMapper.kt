package com.example.alpha_ai.core.data.auth

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject

/** The account operation that failed; some error codes mean different things per operation. */
internal enum class AuthOperation { SignIn, CreateAccount, PasswordReset, Refresh, Other }

/** What an SDK exception says, reduced to what classification needs. */
internal data class AuthErrorSignal(val kind: Kind, val code: String? = null) {
    enum class Kind { Network, TooManyRequests, Auth, Other }
}

/**
 * An SDK failure already reduced to its [signal] by the data source. It carries no message and
 * no cause, so nothing about the user can reach a log through it (FR-024).
 */
internal class AuthServiceException(val signal: AuthErrorSignal) : Exception()

/** A log label for a failure: the class, plus the signal's kind and code when there is one. */
internal fun Exception.logLabel(): String = when (this) {
    is AuthServiceException -> listOfNotNull(signal.kind.name, signal.code).joinToString(" ")
    else -> javaClass.simpleName
}

/** Reads an [AuthErrorSignal] from an SDK exception. Callers handle `CancellationException` first. */
internal fun Throwable.toAuthErrorSignal(): AuthErrorSignal = when (this) {
    is AuthServiceException -> signal
    is FirebaseNetworkException -> AuthErrorSignal(AuthErrorSignal.Kind.Network)
    is FirebaseFirestoreException if code == FirebaseFirestoreException.Code.UNAVAILABLE ->
        AuthErrorSignal(AuthErrorSignal.Kind.Network)
    is FirebaseTooManyRequestsException -> AuthErrorSignal(AuthErrorSignal.Kind.TooManyRequests)
    // The status code is an SDK enum name, safe to log; it tells a denied read from a slow one.
    is FirebaseFirestoreException -> AuthErrorSignal(AuthErrorSignal.Kind.Other, code.name)
    is FirebaseAuthException -> AuthErrorSignal(AuthErrorSignal.Kind.Auth, errorCode)
    else -> AuthErrorSignal(AuthErrorSignal.Kind.Other)
}

/** Classifies service errors as [AuthFailure] (research.md §7). */
internal class FirebaseErrorMapper @Inject constructor() {

    /**
     * The failure for [signal] during [operation], or `null` when the error means success for
     * that operation (an unknown email on password reset, FR-013).
     */
    fun map(signal: AuthErrorSignal, operation: AuthOperation): AuthFailure? = when (signal.kind) {
        AuthErrorSignal.Kind.Network -> AuthFailure.NoConnection
        AuthErrorSignal.Kind.TooManyRequests -> AuthFailure.TooManyAttempts
        AuthErrorSignal.Kind.Other -> AuthFailure.Unknown
        AuthErrorSignal.Kind.Auth -> mapCode(signal.code, operation)
    }

    private fun mapCode(code: String?, operation: AuthOperation): AuthFailure? = when (code) {
        "ERROR_NETWORK_REQUEST_FAILED" -> AuthFailure.NoConnection
        "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> AuthFailure.InvalidCredentials
        "ERROR_USER_NOT_FOUND" -> when (operation) {
            AuthOperation.PasswordReset -> null
            AuthOperation.Refresh -> AuthFailure.SessionExpired
            else -> AuthFailure.InvalidCredentials
        }
        "ERROR_INVALID_EMAIL" -> AuthFailure.InvalidEmail
        "ERROR_EMAIL_ALREADY_IN_USE" -> AuthFailure.EmailAlreadyInUse
        "ERROR_WEAK_PASSWORD" -> AuthFailure.WeakPassword
        "ERROR_TOO_MANY_REQUESTS" -> AuthFailure.TooManyAttempts
        "ERROR_USER_DISABLED" -> AuthFailure.AccountDisabled
        "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> AuthFailure.SessionExpired
        else -> AuthFailure.Unknown
    }
}
