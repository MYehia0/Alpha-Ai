package com.example.alpha_ai.shell

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.alpha_ai.core.domain.auth.StartDestination
import com.example.alpha_ai.feature.auth.navigation.IntroductionRoute
import com.example.alpha_ai.feature.auth.navigation.SignInRoute
import com.example.alpha_ai.ui.main.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The launcher and the single activity that hosts every Compose screen
 * (contracts/app-entry.md "MainActivity start sequence"). The splash screen stays until the
 * start destination is known, so the first frame is already the right screen (FR-001).
 *
 * It is an [AppCompatActivity] so spec 005 can switch the app language with
 * `AppCompatDelegate.setApplicationLocales` on Android 8–12.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val startViewModel: StartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (launchedFromGallery()) {
            showApp(ShellDestinations.startRoute)
            return
        }

        splashScreen.setKeepOnScreenCondition { startViewModel.state.value is StartState.Resolving }
        // Once resolved (also immediately after a configuration change), the first emission is
        // delivered synchronously, so the navigation state is restored as usual.
        lifecycleScope.launch {
            val resolved = startViewModel.state.filterIsInstance<StartState.Resolved>().first()
            when (val destination = resolved.destination) {
                StartDestination.Home -> openHome()
                StartDestination.Introduction -> showApp(IntroductionRoute)
                is StartDestination.SignIn -> showApp(SignInRoute(destination.reason))
            }
        }
    }

    private fun showApp(startRoute: Any) {
        setContent { AlphaApp(startRoute = startRoute, onAuthenticated = ::openHome) }
    }

    /** Hands over to the legacy home (until spec 005) with no transition, so nothing flashes. */
    private fun openHome() {
        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
        startActivity(Intent(this, HomeActivity::class.java), options.toBundle())
        finish()
    }

    /** Debug builds have a "Gallery" launcher alias that opens the component gallery directly. */
    private fun launchedFromGallery(): Boolean =
        intent?.component?.className?.endsWith(GALLERY_ALIAS) == true

    private companion object {
        const val GALLERY_ALIAS = ".GalleryLauncher"
    }
}
