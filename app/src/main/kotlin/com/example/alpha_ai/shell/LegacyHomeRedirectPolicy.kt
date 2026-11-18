package com.example.alpha_ai.shell

import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.core.domain.auth.usecase.RefreshOutcome
import javax.inject.Inject

/**
 * Decides whether, and why, the legacy home leaves for sign-in (contracts/app-entry.md
 * "LegacyHomeRedirectPolicy"). Pure Kotlin. At most one [Decision.Redirect] per instance, so a
 * sign-out never opens sign-in twice or with the wrong notice.
 *
 * Unscoped on purpose: each `HomeActivity` gets a new instance, so a later home can redirect again.
 * Temporary; deleted by spec 005 with the legacy home.
 */
class LegacyHomeRedirectPolicy @Inject constructor() {

    sealed interface Decision {
        data class Redirect(val reason: SignInReason) : Decision

        data object Stay : Decision
    }

    private var redirected = false
    private var signOutInProgress = false
    private var refreshInProgress = false
    private var signedOutDuringRefresh = false

    /** The user confirmed sign-out; the redirect says "signed out", not "session ended". */
    fun onSignOutConfirmed(): Decision {
        signOutInProgress = true
        return redirect(SignInReason.SignedOut)
    }

    /**
     * A session check started. Until its result arrives, a sign-out it causes is left to
     * [onRefreshResult], which knows the reason (an expired session or a disabled account).
     */
    fun onRefreshStarted() {
        refreshInProgress = true
    }

    fun onRefreshResult(outcome: RefreshOutcome): Decision {
        refreshInProgress = false
        return when {
            outcome is RefreshOutcome.SignedOut -> redirect(outcome.reason)
            signedOutDuringRefresh -> redirect(SignInReason.SessionExpired)
            else -> Decision.Stay
        }
    }

    fun onSignedInChanged(signedIn: Boolean): Decision = when {
        signedIn || signOutInProgress -> Decision.Stay
        refreshInProgress -> {
            signedOutDuringRefresh = true
            Decision.Stay
        }
        else -> redirect(SignInReason.SessionExpired)
    }

    private fun redirect(reason: SignInReason): Decision {
        if (redirected) return Decision.Stay
        redirected = true
        return Decision.Redirect(reason)
    }
}
