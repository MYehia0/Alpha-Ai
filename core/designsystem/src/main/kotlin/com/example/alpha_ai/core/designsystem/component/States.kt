package com.example.alpha_ai.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.alpha_ai.core.designsystem.R
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import kotlinx.coroutines.delay

/**
 * Something is in progress. Nothing shows for the first [LOADING_SHOW_DELAY_MS], so a quick
 * operation never flashes an indicator. Offers Cancel when [onCancel] is set.
 *
 * Set [showImmediately] in previews and galleries, where the delay would hide the state.
 */
@Composable
fun AlphaLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
    onCancel: (() -> Unit)? = null,
    showImmediately: Boolean = false,
) {
    var visible by remember { mutableStateOf(showImmediately) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay(LOADING_SHOW_DELAY_MS)
            visible = true
        }
    }
    val motion = AlphaTheme.motion
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible, enter = fadeIn(motion.enter()), exit = fadeOut(motion.exit())) {
            StateColumn {
                CircularProgressIndicator()
                Text(
                    text = message ?: stringResource(R.string.ds_loading),
                    style = AlphaTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (onCancel != null) AlphaTextButton(stringResource(R.string.ds_cancel), onCancel)
            }
        }
    }
}

/** There is nothing to show yet, with one optional action to get started. */
@Composable
fun AlphaEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateColumn(modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AlphaTheme.colors.scheme.onSurfaceVariant,
            modifier = Modifier.size(AlphaTheme.spacing.iconLarge),
        )
        Text(message, style = AlphaTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) AlphaPrimaryButton(actionLabel, onAction)
    }
}

/**
 * Something went wrong. Shows Retry only when [onRetry] is set, which callers do only for
 * failures that retrying can fix. Long messages wrap and Retry stays visible.
 */
@Composable
fun AlphaErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    StateColumn(modifier) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = AlphaTheme.colors.scheme.error,
            modifier = Modifier.size(AlphaTheme.spacing.iconLarge),
        )
        Text(
            text = message,
            style = AlphaTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (onRetry != null) AlphaPrimaryButton(stringResource(R.string.ds_retry), onRetry)
    }
}

@Composable
private fun StateColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = AlphaTheme.spacing.maxContentWidth)
            .padding(AlphaTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.lg),
    ) { content() }
}

@AlphaPreviews
@Composable
private fun StatesPreview() {
    AlphaTheme {
        Surface {
            Column {
                AlphaLoadingState(onCancel = {}, showImmediately = true)
                AlphaEmptyState(
                    message = stringResource(R.string.ds_loading),
                    actionLabel = stringResource(R.string.ds_retry),
                    onAction = {},
                )
                AlphaErrorState(message = stringResource(R.string.ds_in_progress, stringResource(R.string.ds_loading)), onRetry = {})
                AlphaErrorState(message = stringResource(R.string.ds_loading))
            }
        }
    }
}
