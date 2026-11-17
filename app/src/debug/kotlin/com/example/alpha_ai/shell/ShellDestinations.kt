package com.example.alpha_ai.shell

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.alpha_ai.shell.gallery.GalleryRoute
import com.example.alpha_ai.shell.gallery.GalleryScreen
import com.example.alpha_ai.shell.gallery.GallerySectionRoute
import com.example.alpha_ai.shell.gallery.GallerySectionScreen

/** Debug destinations: the component gallery, opened from the "Alpha-AI Gallery" icon. */
object ShellDestinations {
    val startRoute: Any = GalleryRoute

    fun NavGraphBuilder.register(navController: NavHostController) {
        composable<GalleryRoute> {
            GalleryScreen(onOpenSection = { navController.navigate(GallerySectionRoute(it)) })
        }
        composable<GallerySectionRoute> { entry ->
            GallerySectionScreen(
                section = entry.toRoute<GallerySectionRoute>().section,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
