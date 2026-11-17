package com.example.alpha_ai.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * The Alpha-AI theme. Wrap content in it once, at the root of the app or of a preview.
 * Feature code reads colours, type, spacing, shapes, and motion only through [AlphaTheme].
 */
@Composable
fun AlphaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    val colors = remember(darkTheme) { AlphaColors(scheme = scheme, isDark = darkTheme) }
    val reducedMotion = rememberReducedMotion()
    val motion = remember(reducedMotion) { AlphaMotion(reducedMotion) }

    CompositionLocalProvider(
        LocalAlphaColors provides colors,
        LocalAlphaSpacing provides AlphaSpacing(),
        LocalAlphaMotion provides motion,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AlphaTypography,
            shapes = AlphaShapes,
            content = content,
        )
    }
}

/** Read access to the current theme values. */
object AlphaTheme {
    val colors: AlphaColors
        @Composable @ReadOnlyComposable get() = LocalAlphaColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val spacing: AlphaSpacing
        @Composable @ReadOnlyComposable get() = LocalAlphaSpacing.current

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes

    val motion: AlphaMotion
        @Composable @ReadOnlyComposable get() = LocalAlphaMotion.current
}
