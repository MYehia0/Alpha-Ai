package com.example.alpha_ai.core.ui.state

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.alpha_ai.core.designsystem.component.AlphaEmptyState
import com.example.alpha_ai.core.designsystem.component.AlphaErrorState
import com.example.alpha_ai.core.designsystem.component.AlphaLoadingState
import com.example.alpha_ai.core.designsystem.component.loadingHoldMs
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.ui.R
import kotlinx.coroutines.delay

/**
 * Shows the loading, empty, error, or content view for [state]. Once a loading indicator has
 * appeared it stays at least the minimum time, so it never flashes (FR-014).
 */
@Composable
fun <T> AlphaScreenStateHost(
    state: ScreenState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onEmptyAction: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    var shown by remember { mutableStateOf(state) }
    var loadingStartedAt by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(state) {
        if (state is ScreenState.Loading) {
            if (shown !is ScreenState.Loading) loadingStartedAt = SystemClock.uptimeMillis()
        } else if (shown is ScreenState.Loading) {
            delay(loadingHoldMs(SystemClock.uptimeMillis() - loadingStartedAt))
        }
        shown = state
    }

    val motion = AlphaTheme.motion
    AnimatedContent(
        targetState = shown,
        contentKey = { it::class },
        transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
        modifier = modifier,
        label = "screen state",
    ) { current ->
        when (current) {
            is ScreenState.Loading -> AlphaLoadingState(onCancel = onCancel.takeIf { current.cancellable })
            is ScreenState.Empty -> AlphaEmptyState(
                message = stringResource(current.messageRes),
                actionLabel = current.actionLabelRes?.let { stringResource(it) },
                onAction = onEmptyAction,
            )
            is ScreenState.Error -> AlphaErrorState(
                message = stringResource(current.messageRes),
                onRetry = onRetry.takeIf { current.retryable },
            )
            is ScreenState.Content -> content(current.value)
        }
    }
}

@AlphaPreviews
@Composable
private fun ScreenStateHostPreview() {
    AlphaTheme {
        Surface {
            Column {
                AlphaScreenStateHost(ScreenState.Empty(R.string.ai_input_empty), onRetry = {}) {}
                AlphaScreenStateHost(AiFailure.NoConnection.toErrorState(), onRetry = {}) {}
                AlphaScreenStateHost(AiFailure.ServiceUnavailable.toErrorState(), onRetry = {}) {}
                AlphaScreenStateHost(ScreenState.Content(R.string.ai_failure_unknown), onRetry = {}) {
                    Text(stringResource(it))
                }
            }
        }
    }
}
