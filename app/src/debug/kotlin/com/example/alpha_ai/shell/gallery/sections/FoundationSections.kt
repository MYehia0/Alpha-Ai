package com.example.alpha_ai.shell.gallery.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.example.alpha_ai.R
import com.example.alpha_ai.core.designsystem.component.AlphaPrimaryButton
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.core.designsystem.theme.contrastPairs
import com.example.alpha_ai.core.designsystem.theme.contrastRatio

// Token and role names below are identifiers shown to developers, not user copy.

/** Every colour pair the components use, with its measured contrast. */
@Composable
fun ColorsSection() {
    val scheme = AlphaTheme.colors.scheme
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
        contrastPairs(scheme).forEach { pair ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(pair.background, AlphaTheme.shapes.small)
                    .border(AlphaTheme.spacing.borderThin, scheme.outlineVariant, AlphaTheme.shapes.small)
                    .padding(AlphaTheme.spacing.md),
            ) {
                Text(pair.name, color = pair.foreground, style = AlphaTheme.typography.titleSmall)
                Text(
                    text = stringResource(
                        R.string.gallery_contrast,
                        String.format(locale, "%.1f", contrastRatio(pair.foreground, pair.background)),
                    ),
                    color = pair.foreground,
                    style = AlphaTheme.typography.bodySmall,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(AlphaTheme.spacing.iconLarge)
                    .background(AlphaTheme.colors.brandAccent, AlphaTheme.shapes.small),
            )
            Text(
                text = stringResource(R.string.gallery_brand_accent),
                style = AlphaTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = AlphaTheme.spacing.md),
            )
        }
    }
}

/** Every text style, with an English and an Arabic line. */
@Composable
fun TypographySection() {
    val t = AlphaTheme.typography
    val styles: List<Pair<String, TextStyle>> = listOf(
        "displayLarge" to t.displayLarge, "displayMedium" to t.displayMedium, "displaySmall" to t.displaySmall,
        "headlineLarge" to t.headlineLarge, "headlineMedium" to t.headlineMedium, "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge, "titleMedium" to t.titleMedium, "titleSmall" to t.titleSmall,
        "bodyLarge" to t.bodyLarge, "bodyMedium" to t.bodyMedium, "bodySmall" to t.bodySmall,
        "labelLarge" to t.labelLarge, "labelMedium" to t.labelMedium, "labelSmall" to t.labelSmall,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.lg)) {
        styles.forEach { (name, style) ->
            Column {
                Text(name, style = t.labelSmall, color = AlphaTheme.colors.scheme.onSurfaceVariant)
                Text(stringResource(R.string.gallery_sample_latin), style = style)
                Text(stringResource(R.string.gallery_sample_arabic), style = style)
            }
        }
    }
}

/** The spacing steps as bars. */
@Composable
fun SpacingSection() {
    val s = AlphaTheme.spacing
    val steps = listOf("xxs" to s.xxs, "xs" to s.xs, "sm" to s.sm, "md" to s.md, "lg" to s.lg, "xl" to s.xl, "xxl" to s.xxl, "xxxl" to s.xxxl)
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
        steps.forEach { (name, size) -> TokenRow(name, size) { Bar(size) } }
    }
}

/** The shape steps as boxes. */
@Composable
fun ShapesSection() {
    val sh = AlphaTheme.shapes
    val steps: List<Pair<String, Shape>> = listOf(
        "extraSmall" to sh.extraSmall, "small" to sh.small, "medium" to sh.medium,
        "large" to sh.large, "extraLarge" to sh.extraLarge,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md)) {
        steps.forEach { (name, shape) ->
            TokenRow(name, null) {
                Box(
                    Modifier
                        .size(AlphaTheme.spacing.xxxl + AlphaTheme.spacing.xxl)
                        .background(AlphaTheme.colors.scheme.primaryContainer, shape),
                )
            }
        }
    }
}

/** One toggle that animates size, colour, and visibility with the motion tokens. */
@Composable
fun MotionSection() {
    var on by rememberSaveable { mutableStateOf(false) }
    val motion = AlphaTheme.motion
    val scheme = AlphaTheme.colors.scheme
    val size by animateDpAsState(
        targetValue = if (on) AlphaTheme.spacing.xxxl * 2 else AlphaTheme.spacing.xxxl,
        animationSpec = motion.change(),
        label = "size",
    )
    val color by animateColorAsState(
        targetValue = if (on) scheme.primary else scheme.secondaryContainer,
        animationSpec = motion.change(),
        label = "colour",
    )
    Column(verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.lg)) {
        Text(
            text = stringResource(if (motion.reducedMotion) R.string.gallery_reduced_motion_on else R.string.gallery_reduced_motion_off),
            style = AlphaTheme.typography.bodyMedium,
        )
        AlphaPrimaryButton(stringResource(R.string.gallery_motion_play), onClick = { on = !on })
        Box(Modifier.size(size).background(color, AlphaTheme.shapes.medium))
        AnimatedVisibility(on, enter = fadeIn(motion.enter()), exit = fadeOut(motion.exit())) {
            Text(stringResource(R.string.gallery_sample_latin), style = AlphaTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TokenRow(name: String, size: Dp?, sample: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (size == null) name else "$name · ${size.value.toInt()}",
            style = AlphaTheme.typography.labelMedium,
            modifier = Modifier.width(AlphaTheme.spacing.xxxl * 2),
        )
        sample()
    }
}

@Composable
private fun Bar(width: Dp) {
    Box(
        Modifier
            .width(width)
            .height(AlphaTheme.spacing.lg)
            .background(AlphaTheme.colors.scheme.primary),
    )
}

@AlphaPreviews
@Composable
private fun ColorsSectionPreview() = SectionPreview { ColorsSection() }

@AlphaPreviews
@Composable
private fun TypographySectionPreview() = SectionPreview { TypographySection() }

@AlphaPreviews
@Composable
private fun SpacingShapesSectionPreview() = SectionPreview {
    SpacingSection()
    ShapesSection()
}

@AlphaPreviews
@Composable
private fun MotionSectionPreview() = SectionPreview { MotionSection() }

@Composable
internal fun SectionPreview(content: @Composable () -> Unit) {
    AlphaTheme {
        Surface {
            Column(Modifier.padding(AlphaTheme.spacing.lg)) { content() }
        }
    }
}
