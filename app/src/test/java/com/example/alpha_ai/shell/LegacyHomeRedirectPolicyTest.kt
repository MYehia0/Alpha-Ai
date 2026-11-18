package com.example.alpha_ai.shell

import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.core.domain.auth.usecase.RefreshOutcome
import com.example.alpha_ai.shell.LegacyHomeRedirectPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyHomeRedirectPolicyTest {

    private val policy = LegacyHomeRedirectPolicy()

    @Test
    fun `sign-out redirects once with signed out`() {
        assertEquals(Decision.Redirect(SignInReason.SignedOut), policy.onSignOutConfirmed())
        assertEquals(Decision.Stay, policy.onSignedInChanged(false))
    }

    @Test
    fun `refresh that ends the session redirects with its reason`() {
        assertEquals(
            Decision.Redirect(SignInReason.AccountDisabled),
            policy.onRefreshResult(RefreshOutcome.SignedOut(SignInReason.AccountDisabled)),
        )
    }

    @Test
    fun `any other refresh result stays`() {
        assertEquals(Decision.Stay, policy.onRefreshResult(RefreshOutcome.StillSignedIn()))
        assertEquals(Decision.Stay, policy.onRefreshResult(RefreshOutcome.StillSignedIn(AuthFailure.NoConnection)))
    }

    @Test
    fun `losing the session while home is open redirects with session expired`() {
        assertEquals(Decision.Stay, policy.onSignedInChanged(true))
        assertEquals(Decision.Redirect(SignInReason.SessionExpired), policy.onSignedInChanged(false))
    }

    @Test
    fun `refresh session expired then signed out yields one redirect`() {
        assertEquals(
            Decision.Redirect(SignInReason.SessionExpired),
            policy.onRefreshResult(RefreshOutcome.SignedOut(SignInReason.SessionExpired)),
        )
        assertEquals(Decision.Stay, policy.onSignedInChanged(false))
    }

    @Test
    fun `sign-out caused by a refresh waits for the refresh to name the reason`() {
        policy.onRefreshStarted()
        assertEquals(Decision.Stay, policy.onSignedInChanged(false))
        assertEquals(
            Decision.Redirect(SignInReason.AccountDisabled),
            policy.onRefreshResult(RefreshOutcome.SignedOut(SignInReason.AccountDisabled)),
        )
    }

    @Test
    fun `a sign-out seen during a refresh that kept the session still redirects`() {
        policy.onRefreshStarted()
        policy.onSignedInChanged(false)
        assertEquals(
            Decision.Redirect(SignInReason.SessionExpired),
            policy.onRefreshResult(RefreshOutcome.StillSignedIn(AuthFailure.NoConnection)),
        )
    }

    @Test
    fun `a new policy after a redirect can redirect again`() {
        policy.onSignOutConfirmed()
        assertEquals(Decision.Redirect(SignInReason.SessionExpired), LegacyHomeRedirectPolicy().onSignedInChanged(false))
    }
}
