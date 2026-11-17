package com.example.alpha_ai.shell

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme

/**
 * The frame every Compose screen lives in: the theme, one scaffold, and one navigation host.
 *
 * Screens draw the shared [com.example.alpha_ai.core.designsystem.component.AlphaTopBar], which
 * handles the status bar inset; the scaffold handles the other sides. System back pops the back
 * stack and, at the start destination, finishes the activity (FR-019); no custom back handling
 * is needed.
 *
 * To add a screen, see specs/003-design-system-shell/contracts/app-shell.md.
 */
@Composable
fun AlphaApp(navController: NavHostController = rememberNavController()) {
    AlphaTheme {
        // The top bar of each screen handles the status bar; the frame keeps content clear of
        // the navigation bar, display cutouts, and the keyboard.
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = ShellDestinations.startRoute,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            ) {
                with(ShellDestinations) { register(navController) }
                // Spec 004 onward: authGraph(navController), homeGraph(navController), …
            }
        }
    }
}
