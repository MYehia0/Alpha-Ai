package com.example.alpha_ai.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale, on a 4 dp base. Screens use only these steps; `lg` is the screen gutter.
 */
@Immutable
data class AlphaSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** The smallest size of anything the user can tap (constitution VI). */
    val minTouchTarget: Dp = 48.dp,
    /** Icon and indicator sizes: inside a button, standard, and in an empty or error state. */
    val iconSmall: Dp = 18.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 48.dp,
    /** Border and indicator stroke widths. */
    val borderThin: Dp = 1.dp,
    val strokeThin: Dp = 2.dp,
    /** The widest a single column of content grows on large screens. */
    val maxContentWidth: Dp = 600.dp,
)

internal val LocalAlphaSpacing = staticCompositionLocalOf { AlphaSpacing() }
