package com.example.alpha_ai.feature.auth.common

import androidx.annotation.StringRes
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.feature.auth.R

/** The message for each [AuthFailure] (data-model.md "AuthFailure", FR-017). */
object AuthFailureMessages {

    @StringRes
    fun messageRes(failure: AuthFailure): Int = when (failure) {
        AuthFailure.NoConnection -> R.string.auth_error_no_connection
        AuthFailure.InvalidCredentials -> R.string.auth_error_invalid_credentials
        AuthFailure.EmailAlreadyInUse -> R.string.auth_error_email_in_use
        AuthFailure.WeakPassword -> R.string.auth_error_weak_password
        AuthFailure.InvalidEmail -> R.string.auth_error_invalid_email
        AuthFailure.TooManyAttempts -> R.string.auth_error_too_many_attempts
        AuthFailure.AccountDisabled -> R.string.auth_error_account_disabled
        AuthFailure.SessionExpired -> R.string.auth_error_session_expired
        AuthFailure.Unknown -> R.string.auth_error_unknown
    }

    /** Whether trying the same request again can help (the table's "Retry" column). */
    fun isRetryable(failure: AuthFailure): Boolean = when (failure) {
        AuthFailure.NoConnection, AuthFailure.InvalidCredentials, AuthFailure.Unknown -> true
        else -> false
    }

    /** The notice sign-in shows when the user was sent there for [reason]. */
    @StringRes
    fun noticeRes(reason: SignInReason): Int = when (reason) {
        SignInReason.SessionExpired -> R.string.auth_notice_session_expired
        SignInReason.AccountDisabled -> R.string.auth_notice_account_disabled
        SignInReason.SignedOut -> R.string.auth_notice_signed_out
    }
}
