package com.example.alpha_ai.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import com.example.alpha_ai.core.designsystem.R
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme

/** The main action on a screen. */
@Composable
fun AlphaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = { if (buttonAcceptsClick(enabled, busy)) onClick() },
        enabled = enabled,
        modifier = modifier.heightIn(min = AlphaTheme.spacing.minTouchTarget),
        contentPadding = buttonPadding(),
    ) { ButtonContent(text, busy, leadingIcon) }
}

/** A second action next to a primary one. */
@Composable
fun AlphaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = { if (buttonAcceptsClick(enabled, busy)) onClick() },
        enabled = enabled,
        modifier = modifier.heightIn(min = AlphaTheme.spacing.minTouchTarget),
        contentPadding = buttonPadding(),
    ) { ButtonContent(text, busy, leadingIcon) }
}

/** A low-emphasis action. */
@Composable
fun AlphaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    TextButton(
        onClick = { if (buttonAcceptsClick(enabled, busy)) onClick() },
        enabled = enabled,
        modifier = modifier.heightIn(min = AlphaTheme.spacing.minTouchTarget),
        contentPadding = buttonPadding(),
    ) { ButtonContent(text, busy, leadingIcon) }
}

/**
 * An icon-only action, 48 × 48 dp. [contentDescription] is required because there is no text.
 * `AutoMirrored` icons already flip in right-to-left layouts; set [mirrorInRtl] only for other
 * icons that point in a reading direction.
 */
@Composable
fun AlphaIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    mirrorInRtl: Boolean = false,
) {
    require(contentDescription.isNotBlank()) { "An icon button needs a content description." }
    val flip = mirrorInRtl && LocalLayoutDirection.current == LayoutDirection.Rtl
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.graphicsLayer { if (flip) scaleX = -1f },
        )
    }
}

@Composable
private fun buttonPadding() = PaddingValues(
    horizontal = AlphaTheme.spacing.xl,
    vertical = AlphaTheme.spacing.sm,
)

@Composable
private fun RowScope.ButtonContent(text: String, busy: Boolean, leadingIcon: ImageVector?) {
    val busyLabel = stringResource(R.string.ds_in_progress, text)
    when {
        busy -> CircularProgressIndicator(
            modifier = Modifier.size(AlphaTheme.spacing.iconSmall),
            strokeWidth = AlphaTheme.spacing.strokeThin,
            color = LocalContentColor.current,
        )
        leadingIcon != null -> Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(AlphaTheme.spacing.iconSmall),
        )
    }
    if (busy || leadingIcon != null) Spacer(Modifier.size(AlphaTheme.spacing.sm))
    // Labels wrap instead of being cut at large font sizes; the button grows in height.
    Text(
        text = text,
        style = AlphaTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .semantics { if (busy) contentDescription = busyLabel },
    )
}

// Preview labels come from string resources so the Arabic previews show Arabic.
@AlphaPreviews
@Composable
private fun ButtonsPreview() {
    AlphaTheme {
        Surface {
            val label = stringResource(R.string.ds_retry)
            val longLabel = stringResource(R.string.ds_in_progress, stringResource(R.string.ds_retry))
            Column(
                verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm),
                modifier = Modifier.padding(AlphaTheme.spacing.lg),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
                    AlphaPrimaryButton(label, {})
                    AlphaSecondaryButton(label, {})
                    AlphaTextButton(label, {})
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.sm)) {
                    AlphaPrimaryButton(label, {}, enabled = false)
                    AlphaPrimaryButton(label, {}, busy = true)
                    AlphaSecondaryButton(label, {}, busy = true)
                }
                AlphaPrimaryButton(longLabel, {}, leadingIcon = Icons.AutoMirrored.Filled.ArrowForward)
                AlphaIconButton(Icons.Filled.Refresh, label, {})
            }
        }
    }
}
