package com.example.alpha_ai.feature.auth.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.example.alpha_ai.core.domain.auth.SignInReason
import com.example.alpha_ai.feature.auth.R
import com.example.alpha_ai.feature.auth.common.AuthFailureMessages
import com.example.alpha_ai.feature.auth.common.AuthFailureText
import com.example.alpha_ai.feature.auth.common.AuthSubmitButton
import com.example.alpha_ai.feature.auth.common.CollectEffects
import com.example.alpha_ai.feature.auth.common.FieldErrorMessages
import com.example.alpha_ai.feature.auth.common.PasswordField

@Composable
internal fun SignInEntry(
    reason: SignInReason?,
    onAuthenticated: () -> Unit,
    onToSignUp: () -> Unit,
    onToPasswordReset: (String) -> Unit,
    viewModel: SignInViewModel = hiltViewModel<SignInViewModel, SignInViewModel.Factory> { it.create(reason) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SignInEffect.Authenticated -> onAuthenticated()
            SignInEffect.ToSignUp -> onToSignUp()
            is SignInEffect.ToPasswordReset -> onToPasswordReset(effect.email)
        }
    }
    SignInScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun SignInScreen(
    state: SignInUiState,
    onIntent: (SignInIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = !state.submitting
    Column(modifier = modifier.fillMaxSize()) {
        // The bottom of the auth back stack: no back arrow; system back closes the app (FR-021).
        AlphaTopBar(title = stringResource(R.string.auth_sign_in_title))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AlphaTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md),
        ) {
            state.notice?.let { reason ->
                NoticeBanner(reason = reason, onDismiss = { onIntent(SignInIntent.DismissNotice) })
            }
            AlphaTextField(
                value = state.email,
                onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
                label = stringResource(R.string.auth_label_email),
                errorMessage = FieldErrorMessages.message(state.fieldErrors, FormField.Email),
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            )
            PasswordField(
                value = state.password,
                onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
                label = stringResource(R.string.auth_label_password),
                errorMessage = FieldErrorMessages.message(state.fieldErrors, FormField.Password),
                enabled = editable,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onIntent(SignInIntent.Submit) }),
                visible = state.passwordVisible,
                onVisibleChange = { onIntent(SignInIntent.TogglePasswordVisibility) },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AlphaTextButton(
                    text = stringResource(R.string.auth_sign_in_forgot),
                    onClick = { onIntent(SignInIntent.ForgotPassword) },
                    enabled = editable,
                )
            }
            AuthFailureText(
                failure = state.failure,
                actionLabel = if (state.failure == AuthFailure.TooManyAttempts) {
                    stringResource(R.string.auth_action_reset_password)
                } else {
                    null
                },
                onAction = { onIntent(SignInIntent.ForgotPassword) },
            )
            AuthSubmitButton(
                text = stringResource(R.string.auth_sign_in_submit),
                submitting = state.submitting,
                cancellable = state.cancellable,
                onSubmit = { onIntent(SignInIntent.Submit) },
                onCancel = { onIntent(SignInIntent.Cancel) },
            )
            AlphaTextButton(
                text = stringResource(R.string.auth_sign_in_create_account),
                onClick = { onIntent(SignInIntent.CreateAccount) },
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NoticeBanner(reason: SignInReason, onDismiss: () -> Unit) {
    AlphaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(AuthFailureMessages.noticeRes(reason)),
                style = AlphaTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            AlphaTextButton(text = stringResource(R.string.auth_dismiss), onClick = onDismiss)
        }
    }
}

@AlphaPreviews
@Composable
private fun SignInContentPreview() {
    AlphaTheme { SignInScreen(state = SignInUiState(notice = SignInReason.SignedOut), onIntent = {}) }
}

@AlphaPreviews
@Composable
private fun SignInSubmittingPreview() {
    AlphaTheme {
        SignInScreen(
            state = SignInUiState(email = "ali@example.com", password = "secret12", submitting = true, cancellable = true),
            onIntent = {},
        )
    }
}

@AlphaPreviews
@Composable
private fun SignInErrorPreview() {
    AlphaTheme {
        SignInScreen(
            state = SignInUiState(email = "ali@example.com", password = "x", failure = AuthFailure.TooManyAttempts),
            onIntent = {},
        )
    }
}
