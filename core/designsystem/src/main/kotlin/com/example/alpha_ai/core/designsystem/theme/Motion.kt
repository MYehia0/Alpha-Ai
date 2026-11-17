package com.example.alpha_ai.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The named motion styles. All animation uses these, and each returns `snap()` when the system
 * "remove animations" setting is on, so the end state is reached at once (FR-006).
 */
@Immutable
data class AlphaMotion(val reducedMotion: Boolean = false) {
    val durationShort: Int = 150
    val durationMedium: Int = 250
    val durationLong: Int = 400

    val easingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easingEmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val easingEmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Content appearing. */
    fun <T> enter(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMedium, easing = easingEmphasizedDecelerate)

    /** Content leaving. */
    fun <T> exit(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationShort, easing = easingEmphasizedAccelerate)

    /** Content changing in place. */
    fun <T> change(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMedium, easing = easingStandard)
}

internal val LocalAlphaMotion = staticCompositionLocalOf { AlphaMotion() }
