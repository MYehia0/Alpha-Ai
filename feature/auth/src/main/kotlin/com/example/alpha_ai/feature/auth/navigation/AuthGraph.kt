package com.example.alpha_ai.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.alpha_ai.feature.auth.introduction.IntroductionEntry
import com.example.alpha_ai.feature.auth.reset.PasswordResetEntry
import com.example.alpha_ai.feature.auth.signin.SignInEntry
import com.example.alpha_ai.feature.auth.signup.SignUpEntry

/**
 * The onboarding and account screens (contracts/app-entry.md "navigation"). [onAuthenticated]
 * is called once the user is signed in; the host then leaves this graph for home. Sign-in is
 * always the bottom of the auth back stack, so back on sign-in closes the app (FR-021).
 */
fun NavGraphBuilder.authGraph(navController: NavHostController, onAuthenticated: () -> Unit) {
    composable<IntroductionRoute> {
        IntroductionEntry(
            onToSignIn = {
                navController.navigate(SignInRoute()) { popUpTo<IntroductionRoute> { inclusive = true } }
            },
        )
    }
    composable<SignInRoute> { entry ->
        SignInEntry(
            reason = entry.toRoute<SignInRoute>().reason,
            onAuthenticated = onAuthenticated,
            onToSignUp = { navController.navigate(SignUpRoute) },
            onToPasswordReset = { email -> navController.navigate(PasswordResetRoute(email)) },
        )
    }
    composable<SignUpRoute> {
        SignUpEntry(
            onAuthenticated = onAuthenticated,
            onToSignIn = { navController.popBackStack() },
        )
    }
    composable<PasswordResetRoute> { entry ->
        PasswordResetEntry(
            email = entry.toRoute<PasswordResetRoute>().email,
            onToSignIn = { navController.popBackStack() },
        )
    }
}
