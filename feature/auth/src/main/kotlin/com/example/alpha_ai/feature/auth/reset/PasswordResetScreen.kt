package com.example.alpha_ai.feature.auth.reset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alpha_ai.core.designsystem.component.AlphaCard
import com.example.alpha_ai.core.designsystem.component.AlphaTextButton
import com.example.alpha_ai.core.designsystem.component.AlphaTextField
import com.example.alpha_ai.core.designsystem.component.AlphaTopBar
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.feature.auth.R
import com.example.alpha_ai.feature.auth.common.AuthFailureText
import com.example.alpha_ai.feature.auth.common.AuthSubmitButton
import com.example.alpha_ai.feature.auth.common.CollectEffects
import com.example.alpha_ai.feature.auth.common.FieldErrorMessages

@Composable
internal fun PasswordResetEntry(
    email: String,
    onToSignIn: () -> Unit,
    viewModel: PasswordResetViewModel =
        hiltViewModel<PasswordResetViewModel, PasswordResetViewModel.Factory> { it.create(email) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PasswordResetEffect.ToSignIn -> onToSignIn()
        }
    }
    PasswordResetScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun PasswordResetScreen(
    state: PasswordResetUiState,
    onIntent: (PasswordResetIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AlphaTopBar(
            title = stringResource(R.string.auth_reset_title),
            onBack = { onIntent(PasswordResetIntent.BackToSignIn) },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AlphaTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md),
        ) {
            if (state.sent) {
                // The same text for any email, so it does not reveal who has an account (FR-013).
                AlphaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.auth_reset_sent),
                        style = AlphaTheme.typography.bodyLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            } else {
                Text(text = stringResource(R.string.auth_reset_body), style = AlphaTheme.typography.bodyLarge)
            }
            AlphaTextField(
                value = state.email,
                onValueChange = { onIntent(PasswordResetIntent.EmailChanged(it)) },
                label = stringResource(R.string.auth_label_email),
                errorMessage = FieldErrorMessages.message(state.fieldErrors, FormField.Email),
                enabled = !state.submitting,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onIntent(PasswordResetIntent.Submit) }),
            )
            AuthFailureText(failure = state.failure)
            AuthSubmitButton(
                text = when {
                    state.cooldownSeconds > 0 -> stringResource(R.string.auth_reset_send_again_in, state.cooldownSeconds)
                    state.sent -> stringResource(R.string.auth_reset_send_again)
                    else -> stringResource(R.string.auth_reset_submit)
                },
                submitting = state.submitting,
                cancellable = state.cancellable,
                enabled = state.cooldownSeconds == 0,
                onSubmit = { onIntent(PasswordResetIntent.Submit) },
                onCancel = { onIntent(PasswordResetIntent.Cancel) },
            )
            AlphaTextButton(
                text = stringResource(R.string.auth_reset_back_to_sign_in),
                onClick = { onIntent(PasswordResetIntent.BackToSignIn) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@AlphaPreviews
@Composable
private fun PasswordResetContentPreview() {
    AlphaTheme { PasswordResetScreen(state = PasswordResetUiState(email = "ali@example.com"), onIntent = {}) }
}

@AlphaPreviews
@Composable
private fun PasswordResetSentPreview() {
    AlphaTheme {
        PasswordResetScreen(
            state = PasswordResetUiState(email = "ali@example.com", sent = true, cooldownSeconds = 42),
            onIntent = {},
        )
    }
}

@AlphaPreviews
@Composable
private fun PasswordResetErrorPreview() {
    AlphaTheme {
        PasswordResetScreen(
            state = PasswordResetUiState(email = "ali@example.com", failure = AuthFailure.NoConnection),
            onIntent = {},
        )
    }
}
