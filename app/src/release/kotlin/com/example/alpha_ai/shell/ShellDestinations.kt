package com.example.alpha_ai.shell

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.example.alpha_ai.feature.auth.navigation.SignInRoute

/**
 * Release destinations: none of its own. The component gallery and its launcher alias exist only
 * in debug builds, so [startRoute] is used only if something opens the alias path anyway.
 */
object ShellDestinations {
    val startRoute: Any = SignInRoute()

    @Suppress("UnusedReceiverParameter")
    fun NavGraphBuilder.register(@Suppress("UNUSED_PARAMETER") navController: NavHostController) = Unit
}
