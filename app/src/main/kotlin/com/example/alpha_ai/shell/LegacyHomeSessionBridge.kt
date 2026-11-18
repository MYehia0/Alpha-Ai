package com.example.alpha_ai.shell

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.alpha_ai.R
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.usecase.ConsumeConfirmationNoticeUseCase
import com.example.alpha_ai.core.common.DialogAction
import com.example.alpha_ai.core.domain.auth.usecase.SignOutUseCase
import com.example.alpha_ai.core.utils.extensions.showMaterial3Dialog
import com.example.alpha_ai.core.utils.extensions.showMaterial3Snackbar
import com.google.android.material.snackbar.Snackbar
import com.example.alpha_ai.core.domain.auth.usecase.RefreshOutcome
import com.example.alpha_ai.core.domain.auth.usecase.RefreshSessionUseCase
import com.example.alpha_ai.shell.LegacyHomeRedirectPolicy.Decision
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Connects the legacy `HomeActivity` to the new session handling (contracts/app-entry.md
 * "LegacyHomeSessionBridge"): it checks the session on every start and leaves for sign-in when
 * the session ends. Unscoped, so each home gets its own instance and policy. Temporary; deleted
 * by spec 005 with the legacy home.
 */
class LegacyHomeSessionBridge @Inject constructor(
    private val refreshSession: RefreshSessionUseCase,
    private val consumeConfirmationNotice: ConsumeConfirmationNoticeUseCase,
    private val signOut: SignOutUseCase,
    private val authRepository: AuthRepository,
    private val policy: LegacyHomeRedirectPolicy,
) {

    /**
     * Call once from `onCreate`. [signOutButton] asks for confirmation and signs out (FR-016);
     * [snackbarAnchor] shows the one-time "confirmation link sent" message.
     */
    fun attach(
        activity: AppCompatActivity,
        signOutButton: View,
        snackbarAnchor: View,
    ) {
        signOutButton.contentDescription = activity.getString(R.string.home_sign_out_description)
        signOutButton.setOnClickListener { confirmSignOut(activity) }
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    // Main.immediate: the policy hears "refresh started" before any sign-in-state
                    // emission the refresh itself causes.
                    owner.lifecycleScope.launch {
                        policy.onRefreshStarted()
                        val outcome = refreshSession()
                        logUnshown(outcome)
                        act(activity, policy.onRefreshResult(outcome))
                    }
                    // The one-time message after sign-up (FR-008).
                    owner.lifecycleScope.launch {
                        consumeConfirmationNotice()?.let { email ->
                            snackbarAnchor.showMaterial3Snackbar(
                                message = activity.getString(R.string.home_confirmation_link_sent, email),
                                duration = Snackbar.LENGTH_LONG,
                            )
                        }
                    }
                }
            },
        )
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepository.observeSignedIn().collect { signedIn ->
                    act(activity, policy.onSignedInChanged(signedIn))
                }
            }
        }
    }

    private fun confirmSignOut(activity: AppCompatActivity) {
        activity.showMaterial3Dialog(
            title = activity.getString(R.string.home_sign_out_title),
            message = activity.getString(R.string.home_sign_out_message),
            positiveButton = DialogAction(activity.getString(R.string.home_sign_out_confirm)) {
                // The policy hears about the sign-out first, so the signed-in observer does not
                // also open sign-in with "session ended".
                val decision = policy.onSignOutConfirmed()
                activity.lifecycleScope.launch {
                    signOut()
                    act(activity, decision)
                }
            },
            negativeButton = DialogAction(activity.getString(R.string.home_sign_out_cancel)) {},
        )
    }

    private fun act(activity: AppCompatActivity, decision: Decision) {
        if (decision !is Decision.Redirect) return
        val intent = Intent(activity, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(StartViewModel.EXTRA_START_REASON, decision.reason.name)
        activity.startActivity(intent)
    }

    /** A check that failed without ending the session is not shown, so log its case (VIII). */
    private fun logUnshown(outcome: RefreshOutcome) {
        val failure = (outcome as? RefreshOutcome.StillSignedIn)?.unshownFailure ?: return
        Timber.i("Session check not confirmed: %s", failure)
    }
}
