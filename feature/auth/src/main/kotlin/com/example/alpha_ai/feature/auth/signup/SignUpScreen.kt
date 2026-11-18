package com.example.alpha_ai.feature.auth.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alpha_ai.core.designsystem.component.AlphaTextButton
import com.example.alpha_ai.core.designsystem.component.AlphaTextField
import com.example.alpha_ai.core.designsystem.component.AlphaTopBar
import com.example.alpha_ai.core.designsystem.preview.AlphaPreviews
import com.example.alpha_ai.core.designsystem.theme.AlphaTheme
import com.example.alpha_ai.core.domain.auth.AuthFailure
import com.example.alpha_ai.core.domain.auth.FieldError
import com.example.alpha_ai.core.domain.auth.FormField
import com.example.alpha_ai.core.domain.auth.MIN_PASSWORD_LENGTH
import com.example.alpha_ai.feature.auth.R
import com.example.alpha_ai.feature.auth.common.AuthFailureText
import com.example.alpha_ai.feature.auth.common.AuthSubmitButton
import com.example.alpha_ai.feature.auth.common.CollectEffects
import com.example.alpha_ai.feature.auth.common.FieldErrorMessages
import com.example.alpha_ai.feature.auth.common.PasswordField

@Composable
internal fun SignUpEntry(
    onAuthenticated: () -> Unit,
    onToSignIn: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SignUpEffect.Authenticated -> onAuthenticated()
            SignUpEffect.ToSignIn -> onToSignIn()
        }
    }
    SignUpScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun SignUpScreen(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = !state.submitting
    Column(modifier = modifier.fillMaxSize()) {
        AlphaTopBar(
            title = stringResource(R.string.auth_sign_up_title),
            onBack = { onIntent(SignUpIntent.SignInInstead) },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AlphaTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AlphaTheme.spacing.md),
        ) {
            AlphaTextField(
                value = state.name,
                onValueChange = { onIntent(SignUpIntent.NameChanged(it)) },
                label = stringResource(R.string.auth_label_name),
                errorMessage = FieldErrorMessages.message(state.fieldErrors, FormField.Name),
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            AlphaTextField(
                value = state.email,
                onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                label = stringResource(R.string.auth_label_email),
                errorMessage = FieldErrorMessages.message(state.fieldErrors, FormField.Email),
                enabled = editable,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            )
            Column {
                PasswordField(
                    value = state.password,
                    onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                    label = stringResource(R.string.auth_label_password),
                    errorMessage = FieldErrorMessages.message(state.fieldErrors, FormField.Password),
                    enabled = editable,
                    imeAction = ImeAction.Next,
                    visible = state.passwordVisible,
                    onVisibleChange = { onIntent(SignUpIntent.TogglePasswordVisibility) },
                )
                if (FormField.Password !in state.fieldErrors) {
                    Text(
                        text = stringResource(R.string.auth_password_hint, MIN_PASSWORD_LENGTH),
                        style = AlphaTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = AlphaTheme.spacing.lg, top = AlphaTheme.spacing.xs),
                    )
                }
            }
            PasswordField(
                value = state.confirmPassword,
                onValueChange = { onIntent(SignUpIntent.ConfirmPasswordChanged(it)) },
                label = stringResource(R.string.auth_label_confirm_password),
                errorMessage = FieldErrorMessages.message(state.fieldErrors, FormField.ConfirmPassword),
                enabled = editable,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onIntent(SignUpIntent.Submit) }),
                visible = state.passwordVisible,
                onVisibleChange = { onIntent(SignUpIntent.TogglePasswordVisibility) },
            )
            AuthFailureText(
                failure = state.failure,
                actionLabel = if (state.failure == AuthFailure.EmailAlreadyInUse) {
                    stringResource(R.string.auth_action_sign_in)
                } else {
                    null
                },
                onAction = { onIntent(SignUpIntent.SignInInstead) },
            )
            AuthSubmitButton(
                text = stringResource(R.string.auth_sign_up_submit),
                submitting = state.submitting,
                cancellable = state.cancellable,
                onSubmit = { onIntent(SignUpIntent.Submit) },
                onCancel = { onIntent(SignUpIntent.Cancel) },
            )
            AlphaTextButton(
                text = stringResource(R.string.auth_sign_up_have_account),
                onClick = { onIntent(SignUpIntent.SignInInstead) },
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@AlphaPreviews
@Composable
private fun SignUpContentPreview() {
    AlphaTheme {
        SignUpScreen(state = SignUpUiState(name = "Ali", email = "ali@example.com"), onIntent = {})
    }
}

@AlphaPreviews
@Composable
private fun SignUpSubmittingPreview() {
    AlphaTheme {
        SignUpScreen(
            state = SignUpUiState(name = "Ali", email = "ali@example.com", submitting = true, cancellable = true),
            onIntent = {},
        )
    }
}

@AlphaPreviews
@Composable
private fun SignUpErrorPreview() {
    AlphaTheme {
        SignUpScreen(
            state = SignUpUiState(
                email = "ali@example.com",
                fieldErrors = mapOf(FormField.Name to FieldError.Empty, FormField.Password to FieldError.TooShort),
                failure = AuthFailure.EmailAlreadyInUse,
            ),
            onIntent = {},
        )
    }
}
