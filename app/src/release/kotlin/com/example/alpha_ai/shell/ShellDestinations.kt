package com.example.alpha_ai.shell

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.alpha_ai.ui.splash.SplashActivity
import kotlinx.serialization.Serializable

/** Hands over to the legacy flow. Spec 004 replaces it with the session-based start screen. */
@Serializable
data object LegacyEntryRoute

/**
 * Release destinations. Nothing in a release build opens [MainActivity] yet; if something does,
 * the user lands in the normal legacy flow. The component gallery exists only in debug builds.
 */
object ShellDestinations {
    val startRoute: Any = LegacyEntryRoute

    fun NavGraphBuilder.register(@Suppress("UNUSED_PARAMETER") navController: NavHostController) {
        composable<LegacyEntryRoute> {
            val activity = LocalActivity.current
            LaunchedEffect(Unit) {
                activity?.run {
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                }
            }
        }
    }
}
