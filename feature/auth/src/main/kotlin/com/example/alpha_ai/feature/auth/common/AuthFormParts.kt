package com.example.alpha_ai.feature.auth.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.alpha_ai.core.designsystem.component.AlphaPrimaryButton
import com.example.alpha_ai.core.designsystem.component.AlphaTextButton
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.core.domain.auth.AuthFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.example.alpha_ai.core.designsystem.R as DesignR

/**
 * Collects one-off effects while the screen is at least started, on the main thread without
 * delay, so an effect sent during a configuration change is not lost.
 */
@Composable
internal fun <E> CollectEffects(effects: Flow<E>, onEffect: (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val handler by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) { effects.collect { handler(it) } }
        }
    }
}

/**
 * The failure message above the submit button, spoken when it appears, with an optional action
 * such as "Sign in" (FR-017).
 */
@Composable
internal fun AuthFailureText(
    failure: AuthFailure?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    if (failure == null) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(AuthFailureMessages.messageRes(failure)),
            color = MaterialTheme.colorScheme.error,
            style = AlphaTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (actionLabel != null && onAction != null) {
            AlphaTextButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** The submit button with its progress, and Cancel while the request can still be stopped. */
@Composable
internal fun AuthSubmitButton(
    text: String,
    submitting: Boolean,
    cancellable: Boolean,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.xs),
    ) {
        AlphaPrimaryButton(
            text = text,
            onClick = onSubmit,
            enabled = enabled && !submitting,
            busy = submitting,
            modifier = Modifier.fillMaxWidth(),
        )
        if (submitting && cancellable) {
            AlphaTextButton(text = stringResource(DesignR.string.ds_cancel), onClick = onCancel)
        }
    }
}
