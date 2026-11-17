package com.example.alpha_ai.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** The WCAG 2.1 contrast ratio between two opaque colours, from 1.0 to 21.0. */
fun contrastRatio(foreground: Color, background: Color): Double {
    val a = relativeLuminance(foreground)
    val b = relativeLuminance(background)
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

/** WCAG AA minimum for body text. */
const val MIN_TEXT_CONTRAST = 4.5

/** WCAG AA minimum for large text, icons, and borders. */
const val MIN_NON_TEXT_CONTRAST = 3.0

/** A foreground and background that the components place together. */
data class ContrastPair(
    val name: String,
    val foreground: Color,
    val background: Color,
    val minimum: Double,
)

/** Every pair the components use together in [scheme]; ContrastTest checks each one. */
fun contrastPairs(scheme: ColorScheme): List<ContrastPair> = with(scheme) {
    fun text(name: String, fg: Color, bg: Color) = ContrastPair(name, fg, bg, MIN_TEXT_CONTRAST)
    fun nonText(name: String, fg: Color, bg: Color) = ContrastPair(name, fg, bg, MIN_NON_TEXT_CONTRAST)
    listOf(
        text("onPrimary on primary", onPrimary, primary),
        text("onPrimaryContainer on primaryContainer", onPrimaryContainer, primaryContainer),
        text("onSecondary on secondary", onSecondary, secondary),
        text("onSecondaryContainer on secondaryContainer", onSecondaryContainer, secondaryContainer),
        text("onTertiary on tertiary", onTertiary, tertiary),
        text("onTertiaryContainer on tertiaryContainer", onTertiaryContainer, tertiaryContainer),
        text("onBackground on background", onBackground, background),
        text("onSurface on surface", onSurface, surface),
        text("onSurface on surfaceContainerHighest", onSurface, surfaceContainerHighest),
        text("onSurfaceVariant on surface", onSurfaceVariant, surface),
        text("onSurfaceVariant on surfaceVariant", onSurfaceVariant, surfaceVariant),
        text("onSurfaceVariant on surfaceContainerHighest", onSurfaceVariant, surfaceContainerHighest),
        text("primary on surface (text buttons, links)", primary, surface),
        text("primary on surfaceContainerLowest", primary, surfaceContainerLowest),
        text("error on surface (field errors)", error, surface),
        text("onError on error", onError, error),
        text("onErrorContainer on errorContainer", onErrorContainer, errorContainer),
        text("inverseOnSurface on inverseSurface", inverseOnSurface, inverseSurface),
        nonText("outline on surface (borders)", outline, surface),
        nonText("primary on surface (icons, focus)", primary, surface),
    )
}
